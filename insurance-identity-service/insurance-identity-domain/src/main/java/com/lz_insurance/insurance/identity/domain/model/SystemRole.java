package com.lz_insurance.insurance.identity.domain.model;

import com.lz_insurance.core.model.BaseDomainEntity;
import com.lz_insurance.insurance.identity.domain.enums.RoleType;
import com.lz_insurance.insurance.identity.domain.support.DomainGuard;
import lombok.Getter;
import lombok.ToString;

/**
 * A named role. SYSTEM roles are immutable platform defaults; CUSTOM roles are
 * created per tenant. Permissions are attached via {@link RolePermission}
 * (defaults) and overridden per tenant via {@link RolePermissionConfig}.
 */
@Getter
@ToString(callSuper = true)
public class SystemRole extends BaseDomainEntity {

    private final String name;
    private final RoleType roleType;
    private final String description;

    public SystemRole(String name, RoleType roleType, String description) {
        DomainGuard.notBlank(name, "name");
        DomainGuard.notNull(roleType, "roleType");

        this.name = name;
        this.roleType = roleType;
        this.description = description;
    }

    /** Rebuilds a {@code SystemRole} from its persisted state (infrastructure mapper only). */
    public static SystemRole reconstitute(String name, RoleType roleType, String description) {
        return new SystemRole(name, roleType, description);
    }

    public boolean isSystemDefault() {
        return roleType == RoleType.SYSTEM;
    }
}
