package com.lz_insurance.insurance.identity.infrastructure.keycloak;

import com.lz_insurance.insurance.identity.domain.exception.IdentityProviderException;
import com.lz_insurance.insurance.identity.domain.dto.KeycloakUserRegistration;
import com.lz_insurance.insurance.identity.domain.dto.KeycloakUserUpdate;
import com.lz_insurance.security.keycloak.mapper.KeyCloakUserMapper;
import com.lz_insurance.security.keycloak.model.KeycloakProperties;
import com.lz_insurance.security.keycloak.service.KeyCloakService;
import com.lz_insurance.security.keycloak.KeycloakIdentityProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration test for {@link KeycloakUserAdapter} against a real Keycloak (Testcontainers).
 *
 * <p>Proves the full US-M2-001 round-trip through the adapter and the shared
 * {@code insurance-security-keycloak} chain (adapter -> IdentityProvider SPI -> KeyCloakService ->
 * admin client): create user -> exists in the INTERNAL realm -> assign realm role -> disable ->
 * delete. Also proves provider failures surface as {@link IdentityProviderException}.
 *
 * <p>Wired by hand (no Spring context, no Postgres) to keep the test focused on the adapter's real
 * provider behaviour. The image is pinned to 26.x to match {@code keycloak-admin-client} 26.0.8.
 */
@Testcontainers
class KeycloakUserAdapterIT {

    private static final String INTERNAL_REALM = "lz-insurance-internal";
    private static final String AGENT_ROLE = "AGENT";
    private static final String ADMIN = "admin";

    @Container
    private static final GenericContainer<?> KEYCLOAK =
            new GenericContainer<>(DockerImageName.parse("quay.io/keycloak/keycloak:26.0.8"))
                    .withExposedPorts(8080)
                    .withEnv("KC_BOOTSTRAP_ADMIN_USERNAME", ADMIN)
                    .withEnv("KC_BOOTSTRAP_ADMIN_PASSWORD", ADMIN)
                    .withCommand("start-dev")
                    .waitingFor(Wait.forHttp("/realms/master").forPort(8080)
                            .withStartupTimeout(Duration.ofMinutes(3)));

    private static Keycloak adminClient;
    private static KeycloakUserAdapter adapter;

    @BeforeAll
    static void setUp() {
        String serverUrl = "http://" + KEYCLOAK.getHost() + ":" + KEYCLOAK.getMappedPort(8080);

        adminClient = KeycloakBuilder.builder()
                .serverUrl(serverUrl)
                .realm("master")
                .clientId("admin-cli")
                .grantType(OAuth2Constants.PASSWORD)
                .username(ADMIN)
                .password(ADMIN)
                .build();

        // Stand up the internal realm + a realm role to assign.
        RealmRepresentation realm = new RealmRepresentation();
        realm.setRealm(INTERNAL_REALM);
        realm.setEnabled(true);
        adminClient.realms().create(realm);

        RoleRepresentation role = new RoleRepresentation();
        role.setName(AGENT_ROLE);
        adminClient.realm(INTERNAL_REALM).roles().create(role);

        // Operations target the internal realm; auth stays in master (super-admin can manage it).
        KeycloakProperties properties = new KeycloakProperties();
        properties.setServerUrl(serverUrl);
        properties.setRealm(INTERNAL_REALM);

        KeyCloakService keyCloakService = new KeyCloakService(adminClient, properties);
        KeycloakIdentityProvider identityProvider =
                new KeycloakIdentityProvider(keyCloakService, null, properties, new KeyCloakUserMapper());
        adapter = new KeycloakUserAdapter(identityProvider);
    }

    @AfterAll
    static void tearDown() {
        if (adminClient != null) {
            adminClient.close();
        }
    }

    @Test
    @DisplayName("create -> exists in internal realm -> assign role -> disable -> delete")
    void fullLifecycle() {
        String email = "agent.lifecycle@lz-insurance.test";
        KeycloakUserRegistration registration = new KeycloakUserRegistration(
                "agent.lifecycle", email, "Agent", "Lifecycle",
                "Temp-Passw0rd!", false, true);

        // create
        String userId = adapter.createUser(registration);
        assertThat(userId).isNotBlank();

        // exists in the INTERNAL realm, enabled
        UserRepresentation created = adminClient.realm(INTERNAL_REALM).users().get(userId).toRepresentation();
        assertThat(created.getEmail()).isEqualTo(email);
        assertThat(created.isEnabled()).isTrue();

        // assign realm role
        adapter.assignRealmRole(userId, AGENT_ROLE);
        List<String> realmRoles = adminClient.realm(INTERNAL_REALM).users().get(userId)
                .roles().realmLevel().listAll().stream().map(RoleRepresentation::getName).toList();
        assertThat(realmRoles).contains(AGENT_ROLE);

        // disable (modelled as an update with enabled = false)
        adapter.updateUser(userId, KeycloakUserUpdate.disable());
        UserRepresentation disabled = adminClient.realm(INTERNAL_REALM).users().get(userId).toRepresentation();
        assertThat(disabled.isEnabled()).isFalse();

        // delete
        adapter.deleteUser(userId);
        assertThat(adminClient.realm(INTERNAL_REALM).users().searchByEmail(email, true)).isEmpty();
    }

    @Test
    @DisplayName("provider failure is translated into IdentityProviderException")
    void translatesProviderFailure() {
        // Assigning a role to a non-existent user is a provider-level 404; it must not leak as a
        // raw provider exception.
        assertThatThrownBy(() ->
                adapter.assignRealmRole("00000000-0000-0000-0000-000000000000", AGENT_ROLE))
                .isInstanceOf(IdentityProviderException.class);
    }
}
