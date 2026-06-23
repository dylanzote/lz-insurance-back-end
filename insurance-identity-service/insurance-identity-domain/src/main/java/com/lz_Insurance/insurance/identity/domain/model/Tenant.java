package com.lz_Insurance.insurance.identity.domain.model;

import com.lz_Insurance.core.model.BaseDomainEntity;
import com.lz_Insurance.insurance.identity.domain.enumeration.TenantStatus;
import com.lz_Insurance.insurance.identity.domain.exception.InvalidStateTransitionException;
import com.lz_Insurance.insurance.identity.domain.support.DomainGuard;
import lombok.Getter;
import lombok.ToString;

/**
 * Root organizational scope of the platform. Every identity, branch, role and
 * session is owned by exactly one {@code Tenant}. A tenant is created in the
 * ACTIVE state and is not {@code bootstrapped} until its first TENANT_ADMIN and
 * headquarter branch exist.
 */
@Getter
@ToString(callSuper = true)
public class Tenant extends BaseDomainEntity {

    private final String name;
    private final String code;
    private final String country;
    private boolean bootstrapped;
    private String headquarterBranchId;
    private TenantStatus status;

    public Tenant(String name, String code, String country) {
        DomainGuard.notBlank(name, "name");
        DomainGuard.notBlank(code, "code");
        DomainGuard.notBlank(country, "country");
        DomainGuard.isTrue(country.length() == 2, "country",
                "country must be an ISO 3166-1 alpha-2 code");

        this.name = name;
        this.code = code;
        this.country = country.toUpperCase();
        this.bootstrapped = false;
        this.status = TenantStatus.ACTIVE;
    }

    /**
     * One-time bootstrap ceremony marker: records the headquarter branch and
     * flips {@code bootstrapped}. Idempotency (rejecting a second bootstrap) is
     * enforced by the use case, but the model refuses to lose its HQ reference.
     */
    public void markBootstrapped(String headquarterBranchId) {
        DomainGuard.notBlank(headquarterBranchId, "headquarterBranchId");
        this.headquarterBranchId = headquarterBranchId;
        this.bootstrapped = true;
    }

    public void suspend() {
        requireNotDeactivated();
        this.status = TenantStatus.SUSPENDED;
    }

    public void reactivate() {
        requireNotDeactivated();
        this.status = TenantStatus.ACTIVE;
    }

    public void deactivate() {
        this.status = TenantStatus.DEACTIVATED;
    }

    private void requireNotDeactivated() {
        if (status == TenantStatus.DEACTIVATED) {
            throw new InvalidStateTransitionException(
                    status, TenantStatus.ACTIVE, "Tenant");
        }
    }
}
