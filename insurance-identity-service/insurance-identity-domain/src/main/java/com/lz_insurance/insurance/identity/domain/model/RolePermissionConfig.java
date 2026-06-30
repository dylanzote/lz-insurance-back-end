package com.lz_insurance.insurance.identity.domain.model;

import com.lz_insurance.core.model.BaseDomainEntity;
import com.lz_insurance.insurance.identity.domain.enumeration.OrganizationScope;
import com.lz_insurance.insurance.identity.domain.support.DomainGuard;
import lombok.Getter;
import lombok.ToString;

/**
 * Tenant-scoped override of a {@link RolePermission}. When a tenant admin changes
 * the scope a role grants for a permission, that override is recorded here and
 * merged on top of the system default at permission-resolution time.
 */
@Getter
@ToString(callSuper = true)
public class RolePermissionConfig extends BaseDomainEntity {

    private final String tenantId;
    private final String roleId;
    private final String permissionId;
    private final OrganizationScope grantedScope;

    public RolePermissionConfig(String tenantId, String roleId, String permissionId,
                                OrganizationScope grantedScope) {
        DomainGuard.notBlank(tenantId, "tenantId");
        DomainGuard.notBlank(roleId, "roleId");
        DomainGuard.notBlank(permissionId, "permissionId");
        DomainGuard.notNull(grantedScope, "grantedScope");

        this.tenantId = tenantId;
        this.roleId = roleId;
        this.permissionId = permissionId;
        this.grantedScope = grantedScope;
    }

    /** Rebuilds a {@code RolePermissionConfig} from its persisted state (infrastructure mapper only). */
    public static RolePermissionConfig reconstitute(String tenantId, String roleId, String permissionId,
                                                    OrganizationScope grantedScope) {
        return new RolePermissionConfig(tenantId, roleId, permissionId, grantedScope);
    }
}
