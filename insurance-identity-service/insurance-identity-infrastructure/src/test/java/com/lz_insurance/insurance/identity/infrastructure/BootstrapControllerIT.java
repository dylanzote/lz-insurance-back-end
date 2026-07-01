package com.lz_insurance.insurance.identity.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lz_insurance.insurance.identity.domain.enums.BranchType;
import com.lz_insurance.insurance.identity.domain.enums.IdentityStatus;
import com.lz_insurance.insurance.identity.infrastructure.persistence.entity.BranchEntity;
import com.lz_insurance.insurance.identity.infrastructure.persistence.entity.IdentityProfileEntity;
import com.lz_insurance.insurance.identity.infrastructure.persistence.entity.TenantEntity;
import com.lz_insurance.insurance.identity.infrastructure.persistence.repository.BranchJpaRepository;
import com.lz_insurance.insurance.identity.infrastructure.persistence.repository.IdentityProfileJpaRepository;
import com.lz_insurance.insurance.identity.infrastructure.persistence.repository.TenantJpaRepository;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * US-M2-002 exit-gate integration test: the full HTTP flow of {@code POST /identity/bootstrap} against
 * a real Postgres AND a real Keycloak (Testcontainers). Does NOT mock the identity provider — it
 * proves {@code KeycloakAdapterConfig} wires the provider chain and the CLIENT_CREDENTIALS admin
 * client actually provisions users in {@code lz-insurance-internal}, and that the ceremony creates the
 * Tenant + HQ Branch + admin atomically and stores OUR BCrypt hash of the generated credential.
 *
 * <p>Realm setup mirrors how {@code KeyCloakConfig} authenticates (CLIENT_CREDENTIALS against
 * {@code keycloak.realm}), so the internal realm gets a confidential service-account client granted
 * {@code realm-admin} plus the {@code TENANT_ADMIN} realm role. HTTP is driven with the JDK
 * {@link HttpClient} — Boot 4 relocated {@code TestRestTemplate} off the default test classpath.
 *
 * <p>No permission (401/403-via-JWT) assertions: method security is unwired in M2 (G-015). The only
 * 403 tested is the bootstrap-secret gate.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@DisplayName("US-M2-002 — tenant bootstrap ceremony (full HTTP, real Postgres + real Keycloak)")
class BootstrapControllerIT {

    private static final String INTERNAL_REALM = "lz-insurance-internal";
    private static final String TENANT_ADMIN_ROLE = "TENANT_ADMIN";
    private static final String ADMIN_CLIENT_ID = "insurance-identity-admin";
    private static final String ADMIN_CLIENT_SECRET = "identity-admin-secret";
    private static final String KC_ADMIN = "admin";
    private static final String BOOTSTRAP_SECRET = "it-bootstrap-secret";

    private static final ObjectMapper JSON = new ObjectMapper();

    @Container
    static final GenericContainer<?> POSTGRES =
            new GenericContainer<>(DockerImageName.parse("postgres:16-alpine"))
                    .withExposedPorts(5432)
                    .withEnv("POSTGRES_DB", "identity_bootstrap_it")
                    .withEnv("POSTGRES_USER", "test")
                    .withEnv("POSTGRES_PASSWORD", "test")
                    .waitingFor(Wait.forLogMessage(".*database system is ready to accept connections.*", 2));

    @Container
    static final GenericContainer<?> KEYCLOAK =
            new GenericContainer<>(DockerImageName.parse("quay.io/keycloak/keycloak:26.0.8"))
                    .withExposedPorts(8080)
                    .withEnv("KC_BOOTSTRAP_ADMIN_USERNAME", KC_ADMIN)
                    .withEnv("KC_BOOTSTRAP_ADMIN_PASSWORD", KC_ADMIN)
                    .withCommand("start-dev")
                    .waitingFor(Wait.forHttp("/realms/master").forPort(8080)
                            .withStartupTimeout(Duration.ofMinutes(3)));

    private static String keycloakUrl() {
        return "http://" + KEYCLOAK.getHost() + ":" + KEYCLOAK.getMappedPort(8080);
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:postgresql://" + POSTGRES.getHost()
                + ":" + POSTGRES.getFirstMappedPort() + "/identity_bootstrap_it");
        registry.add("spring.datasource.username", () -> "test");
        registry.add("spring.datasource.password", () -> "test");

        registry.add("insurance.security.idp.type", () -> "keycloak");
        registry.add("keycloak.server-url", BootstrapControllerIT::keycloakUrl);
        registry.add("keycloak.realm", () -> INTERNAL_REALM);
        registry.add("keycloak.client-id", () -> ADMIN_CLIENT_ID);
        registry.add("keycloak.client-secret", () -> ADMIN_CLIENT_SECRET);
        registry.add("identity.bootstrap.secret", () -> BOOTSTRAP_SECRET);
    }

    private static Keycloak masterAdmin;
    private final HttpClient http = HttpClient.newHttpClient();

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private TenantJpaRepository tenantRepository;
    @Autowired
    private BranchJpaRepository branchRepository;
    @Autowired
    private IdentityProfileJpaRepository profileRepository;

    @BeforeAll
    static void provisionRealm() {
        masterAdmin = KeycloakBuilder.builder()
                .serverUrl(keycloakUrl())
                .realm("master")
                .clientId("admin-cli")
                .grantType(OAuth2Constants.PASSWORD)
                .username(KC_ADMIN)
                .password(KC_ADMIN)
                .build();

        RealmRepresentation realm = new RealmRepresentation();
        realm.setRealm(INTERNAL_REALM);
        realm.setEnabled(true);
        masterAdmin.realms().create(realm);

        RealmResource internal = masterAdmin.realm(INTERNAL_REALM);

        RoleRepresentation tenantAdminRole = new RoleRepresentation();
        tenantAdminRole.setName(TENANT_ADMIN_ROLE);
        internal.roles().create(tenantAdminRole);

        ClientRepresentation client = new ClientRepresentation();
        client.setClientId(ADMIN_CLIENT_ID);
        client.setSecret(ADMIN_CLIENT_SECRET);
        client.setServiceAccountsEnabled(true);
        client.setStandardFlowEnabled(false);
        client.setDirectAccessGrantsEnabled(false);
        client.setPublicClient(false);
        try (Response ignored = internal.clients().create(client)) {
            // 201 Created — body not needed
        }

        String clientUuid = internal.clients().findByClientId(ADMIN_CLIENT_ID).get(0).getId();
        UserRepresentation serviceAccount = internal.clients().get(clientUuid).getServiceAccountUser();
        String realmMgmtUuid = internal.clients().findByClientId("realm-management").get(0).getId();
        RoleRepresentation realmAdmin = internal.clients().get(realmMgmtUuid)
                .roles().get("realm-admin").toRepresentation();
        internal.users().get(serviceAccount.getId())
                .roles().clientLevel(realmMgmtUuid).add(List.of(realmAdmin));
    }

    @AfterAll
    static void closeAdmin() {
        if (masterAdmin != null) {
            masterAdmin.close();
        }
    }

    @Test
    @DisplayName("valid bootstrap → 200; Tenant + HQ Branch + ACTIVE admin created, hash stored, Keycloak user linked")
    void bootstrapsTenant() throws Exception {
        String email = "admin@acme.test";
        HttpResponse<String> response = post(body("ACME", email), BOOTSTRAP_SECRET);

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode payload = JSON.readTree(response.body());
        String tenantId = payload.get("tenantId").asText();
        String branchId = payload.get("branchId").asText();
        String profileId = payload.get("identityProfileId").asText();
        assertThat(tenantId).isNotBlank();
        assertThat(branchId).isNotBlank();
        assertThat(profileId).isNotBlank();

        // Tenant created + flagged bootstrapped.
        TenantEntity tenant = tenantRepository.findById(tenantId).orElseThrow();
        assertThat(tenant.getCode()).isEqualTo("ACME");
        assertThat(tenant.isBootstrapped()).isTrue();
        assertThat(tenant.getHeadquarterBranchId()).isEqualTo(branchId);

        // Headquarter branch created.
        BranchEntity branch = branchRepository.findById(branchId).orElseThrow();
        assertThat(branch.getType()).isEqualTo(BranchType.HEADQUARTER);
        assertThat(branch.getParentBranchId()).isNull();

        // Admin profile ACTIVE, forced change, linked, with OUR stored BCrypt hash (never the raw password).
        IdentityProfileEntity profile = profileRepository.findById(profileId).orElseThrow();
        assertThat(profile.getStatus()).isEqualTo(IdentityStatus.ACTIVE);
        assertThat(profile.isPasswordChangeRequired()).isTrue();
        assertThat(profile.getCurrentPasswordHash()).isNotBlank().startsWith("$2");

        // Keycloak account exists in the internal realm and is linked to the profile.
        List<UserRepresentation> kcUsers = masterAdmin.realm(INTERNAL_REALM).users().searchByEmail(email, true);
        assertThat(kcUsers).hasSize(1);
        assertThat(profile.getKeycloakUserId()).isEqualTo(kcUsers.get(0).getId());
    }

    @Test
    @DisplayName("second bootstrap of the same tenant code → 409 CONFLICT with ErrorResponse body")
    void rejectsSecondBootstrap() throws Exception {
        String body = body("BETA", "admin@beta.test");
        assertThat(post(body, BOOTSTRAP_SECRET).statusCode()).isEqualTo(200);

        HttpResponse<String> second = post(body, BOOTSTRAP_SECRET);
        assertThat(second.statusCode()).isEqualTo(409);
        assertThat(second.body()).contains("INS-1005"); // DUPLICATE_RESOURCE
    }

    @Test
    @DisplayName("wrong bootstrap secret → 403 FORBIDDEN, nothing created")
    void rejectsWrongSecret() {
        HttpResponse<String> response = post(body("GAMMA", "admin@gamma.test"), "wrong-secret");

        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(response.body()).contains("INS-1004"); // FORBIDDEN
        assertThat(tenantRepository.findByCodeAndDeletedFalse("GAMMA")).isEmpty();
    }

    @Test
    @DisplayName("missing required field (blank country) → 400 validation error")
    void rejectsInvalidRequest() {
        String invalid = """
                {"tenantName":"Delta Insurance","tenantCode":"DELTA","country":"",\
                "hqBranchName":"Delta HQ","hqBranchCode":"DELTA-HQ",\
                "adminEmail":"admin@delta.test","adminFirstName":"Ada","adminLastName":"Lovelace"}""";

        HttpResponse<String> response = post(invalid, BOOTSTRAP_SECRET);

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(tenantRepository.findByCodeAndDeletedFalse("DELTA")).isEmpty();
    }

    // --- helpers ---

    private HttpResponse<String> post(String body, String secret) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/identity/bootstrap"))
                    .header("Content-Type", "application/json")
                    .header("X-Bootstrap-Secret", secret)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            return http.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new IllegalStateException("Bootstrap request failed", e);
        }
    }

    private static String body(String tenantCode, String email) {
        return """
                {"tenantName":"%1$s Insurance","tenantCode":"%1$s","country":"CA",\
                "hqBranchName":"%1$s HQ","hqBranchCode":"%1$s-HQ",\
                "adminEmail":"%2$s","adminFirstName":"Ada","adminLastName":"Lovelace"}"""
                .formatted(tenantCode, email);
    }
}
