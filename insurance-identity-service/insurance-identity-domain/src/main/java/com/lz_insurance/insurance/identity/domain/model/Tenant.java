package com.lz_insurance.insurance.identity.domain.model;

import com.lz_insurance.core.model.BaseDomainEntity;
import com.lz_insurance.insurance.identity.domain.enumeration.TenantStatus;
import com.lz_insurance.insurance.identity.domain.exception.InvalidStateTransitionException;
import com.lz_insurance.insurance.identity.domain.support.DomainGuard;
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
     * Reconstitution constructor for the persistence layer: assigns every field
     * from already-stored, already-valid state. Performs only structural validation
     * (null/format) and does NOT re-apply creation-time defaults.
     */
    private Tenant(String name, String code, String country,
                   boolean bootstrapped, String headquarterBranchId, TenantStatus status) {
        DomainGuard.notBlank(name, "name");
        DomainGuard.notBlank(code, "code");
        DomainGuard.notBlank(country, "country");
        DomainGuard.isTrue(country.length() == 2, "country",
                "country must be an ISO 3166-1 alpha-2 code");
        DomainGuard.notNull(status, "status");

        this.name = name;
        this.code = code;
        this.country = country.toUpperCase();
        this.bootstrapped = bootstrapped;
        this.headquarterBranchId = headquarterBranchId;
        this.status = status;
    }

    /**
     * Rebuilds a {@code Tenant} from its persisted state. Used only by the
     * infrastructure mapper; base/audit fields are restored separately via the
     * inherited setters.
     */
    public static Tenant reconstitute(String name, String code, String country,
                                      boolean bootstrapped, String headquarterBranchId,
                                      TenantStatus status) {
        return new Tenant(name, code, country, bootstrapped, headquarterBranchId, status);
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
