package com.lz_insurance.insurance.identity.domain.model;

import com.lz_insurance.insurance.identity.domain.enumeration.ActorType;
import com.lz_insurance.insurance.identity.domain.enumeration.ApprovalStatus;
import com.lz_insurance.insurance.identity.domain.enumeration.BranchStatus;
import com.lz_insurance.insurance.identity.domain.enumeration.BranchType;
import com.lz_insurance.insurance.identity.domain.enumeration.ExternalUserType;
import com.lz_insurance.insurance.identity.domain.enumeration.IdentityStatus;
import com.lz_insurance.insurance.identity.domain.enumeration.InternalUserType;
import com.lz_insurance.insurance.identity.domain.enumeration.OrganizationScope;
import com.lz_insurance.insurance.identity.domain.enumeration.PermissionAction;
import com.lz_insurance.insurance.identity.domain.enumeration.PermissionResource;
import com.lz_insurance.insurance.identity.domain.enumeration.RoleType;
import com.lz_insurance.insurance.identity.domain.enumeration.SessionStatus;
import com.lz_insurance.insurance.identity.domain.enumeration.TenantStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the {@code reconstitute(...)} factories restore stored, already-valid state
 * (including non-default status and historical timestamps) without re-applying the
 * creation-time starting state, and that the inherited base/audit fields survive the
 * round trip via their setters (the path the infrastructure mapper uses).
 */
class ReconstitutionTest {

    @Test
    @DisplayName("Tenant reconstitutes a bootstrapped, SUSPENDED state without forcing ACTIVE")
    void tenant() {
        Tenant tenant = Tenant.reconstitute("Acme", "ACME", "CA", true, "branch-hq", TenantStatus.SUSPENDED);

        assertThat(tenant.isBootstrapped()).isTrue();
        assertThat(tenant.getHeadquarterBranchId()).isEqualTo("branch-hq");
        assertThat(tenant.getStatus()).isEqualTo(TenantStatus.SUSPENDED);
    }

    @Test
    @DisplayName("base/audit fields restored via inherited setters survive on a reconstituted aggregate")
    void baseFieldsRestorable() {
        Tenant tenant = Tenant.reconstitute("Acme", "ACME", "CA", false, null, TenantStatus.ACTIVE);
        LocalDateTime created = LocalDateTime.of(2024, 1, 1, 9, 0);

        tenant.setId("tenant-001");
        tenant.setVersion(7L);
        tenant.setCreatedAt(created);
        tenant.setCreatedBy("admin");
        tenant.setDeleted(true);

        assertThat(tenant.getId()).isEqualTo("tenant-001");
        assertThat(tenant.getVersion()).isEqualTo(7L);
        assertThat(tenant.getCreatedAt()).isEqualTo(created);
        assertThat(tenant.getCreatedBy()).isEqualTo("admin");
        assertThat(tenant.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("Branch reconstitutes an INACTIVE branch and keeps the HQ structural rule")
    void branch() {
        Branch branch = Branch.reconstitute("tenant-1", "branch-parent", "Downtown", "DT",
                BranchType.LOCAL, BranchStatus.INACTIVE);

        assertThat(branch.getStatus()).isEqualTo(BranchStatus.INACTIVE);
        assertThat(branch.isTopLevel()).isFalse();
    }

    @Test
    @DisplayName("IdentityProfile reconstitutes an ACTIVE internal profile with its Keycloak id (no PENDING re-trigger)")
    void identityProfileInternal() {
        IdentityProfile profile = IdentityProfile.reconstitute("tenant-1", "branch-1", ActorType.INTERNAL,
                InternalUserType.AGENT, null, "agent@lz.test", "Ada", "Lovelace",
                "kc-123", IdentityStatus.ACTIVE, true);

        assertThat(profile.getStatus()).isEqualTo(IdentityStatus.ACTIVE);
        assertThat(profile.getKeycloakUserId()).isEqualTo("kc-123");
        assertThat(profile.getInternalUserType()).isEqualTo(InternalUserType.AGENT);
        assertThat(profile.getExternalUserType()).isNull();
        assertThat(profile.isPasswordChangeRequired()).isTrue();
    }

    @Test
    @DisplayName("IdentityProfile reconstitutes an external profile without a branch")
    void identityProfileExternal() {
        IdentityProfile profile = IdentityProfile.reconstitute("tenant-1", null, ActorType.EXTERNAL,
                null, ExternalUserType.POLICYHOLDER, "ph@lz.test", "Grace", "Hopper",
                null, IdentityStatus.PENDING_APPROVAL, false);

        assertThat(profile.getExternalUserType()).isEqualTo(ExternalUserType.POLICYHOLDER);
        assertThat(profile.getBranchId()).isNull();
        assertThat(profile.getKeycloakUserId()).isNull();
        assertThat(profile.isPasswordChangeRequired()).isFalse();
    }

    @Test
    @DisplayName("ApprovalRequest reconstitutes an APPROVED decision and preserves the original requestedAt")
    void approvalRequest() {
        LocalDateTime requestedAt = LocalDateTime.of(2024, 3, 10, 8, 0);
        LocalDateTime reviewedAt = LocalDateTime.of(2024, 3, 11, 14, 30);

        ApprovalRequest request = ApprovalRequest.reconstitute("profile-1", "requester-1", "please review",
                requestedAt, ApprovalStatus.APPROVED, "reviewer-1", "looks good", reviewedAt);

        assertThat(request.getStatus()).isEqualTo(ApprovalStatus.APPROVED);
        assertThat(request.getRequestedAt()).isEqualTo(requestedAt);
        assertThat(request.getReviewedAt()).isEqualTo(reviewedAt);
        assertThat(request.getReviewedById()).isEqualTo("reviewer-1");
        assertThat(request.isPending()).isFalse();
    }

    @Test
    @DisplayName("SessionRegistry reconstitutes a REVOKED session that is no longer valid")
    void sessionRegistry() {
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(1);
        LocalDateTime lastActivity = LocalDateTime.of(2024, 5, 1, 12, 0);

        SessionRegistry session = SessionRegistry.reconstitute("profile-1", "tenant-1", "hashed-token",
                "10.0.0.1", "JUnit", expiresAt, SessionStatus.REVOKED, lastActivity);

        assertThat(session.getStatus()).isEqualTo(SessionStatus.REVOKED);
        assertThat(session.getLastActivityAt()).isEqualTo(lastActivity);
        assertThat(session.isValid()).isFalse();
    }

    @Test
    @DisplayName("RBAC value models reconstitute their stored fields")
    void rbacModels() {
        SystemRole role = SystemRole.reconstitute("AGENT", RoleType.SYSTEM, "Front-line agent");
        assertThat(role.isSystemDefault()).isTrue();

        Permission permission = Permission.reconstitute(PermissionResource.POLICY, PermissionAction.READ,
                OrganizationScope.BRANCH);
        assertThat(permission.key()).isEqualTo("POLICY:READ:BRANCH");

        RolePermission rolePermission = RolePermission.reconstitute("role-1", "perm-1", OrganizationScope.ALL);
        assertThat(rolePermission.getGrantedScope()).isEqualTo(OrganizationScope.ALL);

        RolePermissionConfig config = RolePermissionConfig.reconstitute("tenant-1", "role-1", "perm-1",
                OrganizationScope.OWN);
        assertThat(config.getGrantedScope()).isEqualTo(OrganizationScope.OWN);

        ProfileRoleAssignment assignment = ProfileRoleAssignment.reconstitute("profile-1", "role-1",
                "tenant-1", null);
        assertThat(assignment.isTenantWide()).isTrue();
    }
}
