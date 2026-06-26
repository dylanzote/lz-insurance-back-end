package com.lz_Insurance.insurance.identity.domain.model;

import com.lz_Insurance.core.model.BaseDomainEntity;
import com.lz_Insurance.insurance.identity.domain.enumeration.BranchStatus;
import com.lz_Insurance.insurance.identity.domain.enumeration.BranchType;
import com.lz_Insurance.insurance.identity.domain.support.DomainGuard;
import lombok.Getter;
import lombok.ToString;

/**
 * A node in a tenant's organizational hierarchy. A {@code HEADQUARTER} branch is
 * the root and must not declare a {@code parentBranchId}; REGIONAL and LOCAL
 * branches hang beneath a parent to form sub-branch trees.
 */
@Getter
@ToString(callSuper = true)
public class Branch extends BaseDomainEntity {

    private final String tenantId;
    private final String parentBranchId;
    private final String name;
    private final String code;
    private final BranchType type;
    private BranchStatus status;

    public Branch(String tenantId, String parentBranchId, String name, String code, BranchType type) {
        DomainGuard.notBlank(tenantId, "tenantId");
        DomainGuard.notBlank(name, "name");
        DomainGuard.notBlank(code, "code");
        DomainGuard.notNull(type, "type");

        if (type == BranchType.HEADQUARTER) {
            DomainGuard.isNull(parentBranchId, "parentBranchId",
                    "a HEADQUARTER branch cannot have a parentBranchId");
        }

        this.tenantId = tenantId;
        this.parentBranchId = parentBranchId;
        this.name = name;
        this.code = code;
        this.type = type;
        this.status = BranchStatus.ACTIVE;
    }

    /**
     * Reconstitution constructor for the persistence layer: structural validation
     * only, restores the stored status rather than forcing ACTIVE.
     */
    private Branch(String tenantId, String parentBranchId, String name, String code,
                   BranchType type, BranchStatus status) {
        DomainGuard.notBlank(tenantId, "tenantId");
        DomainGuard.notBlank(name, "name");
        DomainGuard.notBlank(code, "code");
        DomainGuard.notNull(type, "type");
        DomainGuard.notNull(status, "status");

        if (type == BranchType.HEADQUARTER) {
            DomainGuard.isNull(parentBranchId, "parentBranchId",
                    "a HEADQUARTER branch cannot have a parentBranchId");
        }

        this.tenantId = tenantId;
        this.parentBranchId = parentBranchId;
        this.name = name;
        this.code = code;
        this.type = type;
        this.status = status;
    }

    /** Rebuilds a {@code Branch} from its persisted state (infrastructure mapper only). */
    public static Branch reconstitute(String tenantId, String parentBranchId, String name,
                                      String code, BranchType type, BranchStatus status) {
        return new Branch(tenantId, parentBranchId, name, code, type, status);
    }

    public boolean isTopLevel() {
        return parentBranchId == null;
    }

    public void activate() {
        this.status = BranchStatus.ACTIVE;
    }

    public void deactivate() {
        this.status = BranchStatus.INACTIVE;
    }
}
