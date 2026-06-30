package com.lz_insurance.insurance.identity.domain.model;

import com.lz_insurance.core.model.BaseDomainEntity;
import com.lz_insurance.insurance.identity.domain.support.DomainGuard;
import lombok.Getter;
import lombok.ToString;

/**
 * Binds a role to an {@link IdentityProfile} within a tenant. A null {@code branchId}
 * means the assignment is tenant-wide; a non-null {@code branchId} confines the role
 * to a single branch.
 */
@Getter
@ToString(callSuper = true)
public class ProfileRoleAssignment extends BaseDomainEntity {

    private final String identityProfileId;
    private final String roleId;
    private final String tenantId;
    private final String branchId;

    public ProfileRoleAssignment(String identityProfileId, String roleId, String tenantId, String branchId) {
        DomainGuard.notBlank(identityProfileId, "identityProfileId");
        DomainGuard.notBlank(roleId, "roleId");
        DomainGuard.notBlank(tenantId, "tenantId");

        this.identityProfileId = identityProfileId;
        this.roleId = roleId;
        this.tenantId = tenantId;
        this.branchId = branchId;
    }

    /** Rebuilds a {@code ProfileRoleAssignment} from its persisted state (infrastructure mapper only). */
    public static ProfileRoleAssignment reconstitute(String identityProfileId, String roleId, String tenantId, String branchId) {
        return new ProfileRoleAssignment(identityProfileId, roleId, tenantId, branchId);
    }

    public boolean isTenantWide() {
        return branchId == null;
    }
}
