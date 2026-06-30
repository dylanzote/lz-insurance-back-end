package com.lz_insurance.insurance.identity.domain.model;

import com.lz_insurance.core.model.BaseDomainEntity;
import com.lz_insurance.insurance.identity.domain.enumeration.OrganizationScope;
import com.lz_insurance.insurance.identity.domain.support.DomainGuard;
import lombok.Getter;
import lombok.ToString;

/**
 * System-default junction: binds a {@link Permission} to a {@link SystemRole} at a
 * {@code grantedScope}. These are the platform defaults; tenants may override them
 * via {@link RolePermissionConfig}, with defaults winning when no override exists.
 */
@Getter
@ToString(callSuper = true)
public class RolePermission extends BaseDomainEntity {

    private final String roleId;
    private final String permissionId;
    private final OrganizationScope grantedScope;

    public RolePermission(String roleId, String permissionId, OrganizationScope grantedScope) {
        DomainGuard.notBlank(roleId, "roleId");
        DomainGuard.notBlank(permissionId, "permissionId");
        DomainGuard.notNull(grantedScope, "grantedScope");

        this.roleId = roleId;
        this.permissionId = permissionId;
        this.grantedScope = grantedScope;
    }

    /** Rebuilds a {@code RolePermission} from its persisted state (infrastructure mapper only). */
    public static RolePermission reconstitute(String roleId, String permissionId,
                                              OrganizationScope grantedScope) {
        return new RolePermission(roleId, permissionId, grantedScope);
    }
}
