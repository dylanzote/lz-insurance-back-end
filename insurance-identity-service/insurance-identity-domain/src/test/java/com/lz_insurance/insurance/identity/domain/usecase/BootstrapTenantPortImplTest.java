package com.lz_insurance.insurance.identity.domain.usecase;

import com.lz_insurance.core.context.RequestContext;
import com.lz_insurance.core.exception.ErrorCode;
import com.lz_insurance.core.exception.InsuranceException;
import com.lz_insurance.insurance.identity.domain.dto.BootstrapResult;
import com.lz_insurance.insurance.identity.domain.dto.CreateAdminTenantRequest;
import com.lz_insurance.insurance.identity.domain.dto.KeycloakUserRegistration;
import com.lz_insurance.insurance.identity.domain.enums.ActorType;
import com.lz_insurance.insurance.identity.domain.enums.BranchType;
import com.lz_insurance.insurance.identity.domain.enums.IdentityStatus;
import com.lz_insurance.insurance.identity.domain.model.Branch;
import com.lz_insurance.insurance.identity.domain.model.IdentityProfile;
import com.lz_insurance.insurance.identity.domain.model.Tenant;
import com.lz_insurance.insurance.identity.domain.port.out.BranchRepository;
import com.lz_insurance.insurance.identity.domain.port.out.IdentityProfileRepository;
import com.lz_insurance.insurance.identity.domain.port.out.KeycloakUserPort;
import com.lz_insurance.insurance.identity.domain.port.out.PasswordHasher;
import com.lz_insurance.insurance.identity.domain.port.out.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("BootstrapTenantPortImpl — atomic create Tenant + HQ Branch + first TENANT_ADMIN (US-M2-002)")
class BootstrapTenantPortImplTest {

    private static final String TENANT_CODE = "ACME";
    private static final String HASH = "bcrypt-hash";

    @Mock private TenantRepository tenantRepository;
    @Mock private BranchRepository branchRepository;
    @Mock private IdentityProfileRepository identityProfileRepository;
    @Mock private KeycloakUserPort keycloakUserPort;
    @Mock private PasswordHasher passwordHasher;

    private BootstrapTenantPortImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new BootstrapTenantPortImpl(tenantRepository, branchRepository,
                identityProfileRepository, keycloakUserPort, passwordHasher);
    }

    private CreateAdminTenantRequest request() {
        return new CreateAdminTenantRequest("Acme Insurance", TENANT_CODE, "CA",
                "Acme HQ", "ACME-HQ", "admin@acme.test", "Ada", "Lovelace");
    }

    private RequestContext context() {
        return RequestContext.builder()
                .correlationId("corr-1").ipAddress("127.0.0.1").userAgent("junit")
                .requestedAt(Instant.now()).build();
    }

    /** Stamps ids on newly-saved aggregates, mimicking persistence. */
    private void stubSavesWithGeneratedIds() {
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> {
            Tenant t = inv.getArgument(0);
            if (t.getId() == null) {
                t.setId("tenant-1");
            }
            return t;
        });
        when(branchRepository.save(any(Branch.class))).thenAnswer(inv -> {
            Branch b = inv.getArgument(0);
            if (b.getId() == null) {
                b.setId("branch-1");
            }
            return b;
        });
        when(identityProfileRepository.save(any(IdentityProfile.class))).thenAnswer(inv -> {
            IdentityProfile p = inv.getArgument(0);
            if (p.getId() == null) {
                p.setId("profile-1");
            }
            return p;
        });
    }

    @Test
    @DisplayName("happy path: creates HQ branch + ACTIVE admin (passwordChangeRequired + stored hash), flags tenant")
    void bootstrapsNewTenant() {
        when(tenantRepository.findByCode(TENANT_CODE)).thenReturn(Optional.empty());
        stubSavesWithGeneratedIds();
        when(keycloakUserPort.createUser(any(KeycloakUserRegistration.class))).thenReturn("kc-user-1");
        when(passwordHasher.encrypt(any())).thenReturn(HASH);

        BootstrapResult result = useCase.createTenantAdmin(request(), context());

        assertThat(result.tenantId()).isEqualTo("tenant-1");
        assertThat(result.branchId()).isEqualTo("branch-1");
        assertThat(result.identityProfileId()).isEqualTo("profile-1");
        assertThat(result.keycloakUserId()).isEqualTo("kc-user-1");

        // HQ branch is a top-level HEADQUARTER.
        ArgumentCaptor<Branch> branch = ArgumentCaptor.forClass(Branch.class);
        verify(branchRepository).save(branch.capture());
        assertThat(branch.getValue().getType()).isEqualTo(BranchType.HEADQUARTER);
        assertThat(branch.getValue().getParentBranchId()).isNull();

        // Keycloak got a generated, policy-length password; role assigned; no compensation.
        ArgumentCaptor<KeycloakUserRegistration> reg = ArgumentCaptor.forClass(KeycloakUserRegistration.class);
        verify(keycloakUserPort).createUser(reg.capture());
        assertThat(reg.getValue().temporaryPassword()).hasSizeGreaterThanOrEqualTo(12);
        verify(keycloakUserPort).assignRealmRole("kc-user-1", "TENANT_ADMIN");
        verify(passwordHasher).encrypt(reg.getValue().temporaryPassword());
        verify(keycloakUserPort, never()).deleteUser(any());

        // Admin profile is ACTIVE, forced to change password, and carries OUR stored hash.
        ArgumentCaptor<IdentityProfile> profile = ArgumentCaptor.forClass(IdentityProfile.class);
        verify(identityProfileRepository, atLeastOnce()).save(profile.capture());
        IdentityProfile persisted = profile.getValue();
        assertThat(persisted.getActorType()).isEqualTo(ActorType.INTERNAL);
        assertThat(persisted.getStatus()).isEqualTo(IdentityStatus.ACTIVE);
        assertThat(persisted.isPasswordChangeRequired()).isTrue();
        assertThat(persisted.getCurrentPasswordHash()).isEqualTo(HASH);

        // Tenant flagged bootstrapped with its HQ branch.
        ArgumentCaptor<Tenant> tenant = ArgumentCaptor.forClass(Tenant.class);
        verify(tenantRepository, atLeastOnce()).save(tenant.capture());
        assertThat(tenant.getValue().isBootstrapped()).isTrue();
        assertThat(tenant.getValue().getHeadquarterBranchId()).isEqualTo("branch-1");
    }

    @Test
    @DisplayName("existing bootstrapped tenant code → DUPLICATE_RESOURCE (409), nothing created")
    void rejectsAlreadyBootstrapped() {
        Tenant existing = new Tenant("Acme Insurance", TENANT_CODE, "CA");
        existing.setId("tenant-1");
        existing.markBootstrapped("branch-1");
        when(tenantRepository.findByCode(TENANT_CODE)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> useCase.createTenantAdmin(request(), context()))
                .isInstanceOf(InsuranceException.class)
                .satisfies(ex -> assertThat(((InsuranceException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.DUPLICATE_RESOURCE));

        verifyNoInteractions(keycloakUserPort, branchRepository, identityProfileRepository);
        verify(tenantRepository, never()).save(any());
    }

    @Test
    @DisplayName("existing NON-bootstrapped tenant code (partial prior attempt) → 409, no silent resume")
    void rejectsPartialBootstrap() {
        Tenant partial = new Tenant("Acme Insurance", TENANT_CODE, "CA");
        partial.setId("tenant-1"); // bootstrapped == false
        when(tenantRepository.findByCode(TENANT_CODE)).thenReturn(Optional.of(partial));

        assertThatThrownBy(() -> useCase.createTenantAdmin(request(), context()))
                .isInstanceOf(InsuranceException.class)
                .satisfies(ex -> assertThat(((InsuranceException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.DUPLICATE_RESOURCE));

        verifyNoInteractions(keycloakUserPort, branchRepository, identityProfileRepository);
        verify(tenantRepository, never()).save(any());
    }

    @Test
    @DisplayName("Keycloak createUser failure → Phase-1 records compensated (deleted), no role assigned, error propagates")
    void compensatesWhenCreateUserFails() {
        when(tenantRepository.findByCode(TENANT_CODE)).thenReturn(Optional.empty());
        stubSavesWithGeneratedIds();
        when(keycloakUserPort.createUser(any(KeycloakUserRegistration.class)))
                .thenThrow(new RuntimeException("keycloak down"));

        assertThatThrownBy(() -> useCase.createTenantAdmin(request(), context()))
                .isInstanceOf(RuntimeException.class);

        verify(identityProfileRepository).deleteById("profile-1");
        verify(branchRepository).deleteById("branch-1");
        verify(tenantRepository).deleteById("tenant-1");
        verify(keycloakUserPort, never()).assignRealmRole(any(), any());
        verify(keycloakUserPort, never()).deleteUser(any());
    }

    @Test
    @DisplayName("assignRealmRole failure → Keycloak user deleted + Phase-1 records compensated, error propagates")
    void compensatesWhenAssignRoleFails() {
        when(tenantRepository.findByCode(TENANT_CODE)).thenReturn(Optional.empty());
        stubSavesWithGeneratedIds();
        when(keycloakUserPort.createUser(any(KeycloakUserRegistration.class))).thenReturn("kc-user-1");
        lenient().when(passwordHasher.encrypt(any())).thenReturn(HASH);
        doThrow(new RuntimeException("no such role"))
                .when(keycloakUserPort).assignRealmRole("kc-user-1", "TENANT_ADMIN");

        assertThatThrownBy(() -> useCase.createTenantAdmin(request(), context()))
                .isInstanceOf(RuntimeException.class);

        verify(keycloakUserPort).deleteUser("kc-user-1");
        verify(identityProfileRepository).deleteById("profile-1");
        verify(branchRepository).deleteById("branch-1");
        verify(tenantRepository).deleteById("tenant-1");
    }
}
