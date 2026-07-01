package com.lz_insurance.insurance.identity.domain.model;

import com.lz_insurance.insurance.identity.domain.enums.OrganizationScope;
import com.lz_insurance.insurance.identity.domain.enums.PermissionAction;
import com.lz_insurance.insurance.identity.domain.enums.PermissionResource;
import com.lz_insurance.insurance.identity.domain.enums.RoleType;
import com.lz_insurance.insurance.identity.domain.exception.DomainValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Field-integrity tests for the RBAC value-style models. */
class RbacModelsTest {

    @Test
    @DisplayName("SystemRole reports SYSTEM defaults and rejects a blank name")
    void systemRole() {
        SystemRole role = new SystemRole("AGENT", RoleType.SYSTEM, "Front-line agent");

        assertThat(role.getName()).isEqualTo("AGENT");
        assertThat(role.isSystemDefault()).isTrue();

        assertThatThrownBy(() -> new SystemRole(" ", RoleType.SYSTEM, null))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> new SystemRole("X", null, null))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    @DisplayName("Permission exposes a canonical resource:action:scope key")
    void permission() {
        Permission permission = new Permission(
                PermissionResource.POLICY, PermissionAction.READ, OrganizationScope.OWN);

        assertThat(permission.key()).isEqualTo("POLICY:READ:OWN");

        assertThatThrownBy(() -> new Permission(null, PermissionAction.READ, OrganizationScope.OWN))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    @DisplayName("RolePermission binds a role to a permission at a scope")
    void rolePermission() {
        RolePermission rp = new RolePermission("role-1", "perm-1", OrganizationScope.BRANCH);

        assertThat(rp.getRoleId()).isEqualTo("role-1");
        assertThat(rp.getPermissionId()).isEqualTo("perm-1");
        assertThat(rp.getGrantedScope()).isEqualTo(OrganizationScope.BRANCH);

        assertThatThrownBy(() -> new RolePermission("", "perm-1", OrganizationScope.BRANCH))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    @DisplayName("RolePermissionConfig is tenant-scoped and validates its keys")
    void rolePermissionConfig() {
        RolePermissionConfig config = new RolePermissionConfig(
                "tenant-1", "role-1", "perm-1", OrganizationScope.ALL);

        assertThat(config.getTenantId()).isEqualTo("tenant-1");
        assertThat(config.getGrantedScope()).isEqualTo(OrganizationScope.ALL);

        assertThatThrownBy(() -> new RolePermissionConfig("", "role-1", "perm-1", OrganizationScope.ALL))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> new RolePermissionConfig("tenant-1", "role-1", "perm-1", null))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    @DisplayName("ProfileRoleAssignment is tenant-wide when branchId is null")
    void profileRoleAssignment() {
        ProfileRoleAssignment wide = new ProfileRoleAssignment("profile-1", "role-1", "tenant-1", null);
        assertThat(wide.isTenantWide()).isTrue();

        ProfileRoleAssignment scoped = new ProfileRoleAssignment("profile-1", "role-1", "tenant-1", "branch-1");
        assertThat(scoped.isTenantWide()).isFalse();

        assertThatThrownBy(() -> new ProfileRoleAssignment("", "role-1", "tenant-1", null))
                .isInstanceOf(DomainValidationException.class);
    }
}
