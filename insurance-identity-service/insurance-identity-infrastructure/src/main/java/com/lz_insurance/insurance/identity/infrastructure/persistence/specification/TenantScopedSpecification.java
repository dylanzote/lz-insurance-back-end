package com.lz_insurance.insurance.identity.infrastructure.persistence.specification;

import com.lz_Insurance.persistence.entity.BaseJpaEntity;
import com.lz_Insurance.persistence.specification.BaseSpecification;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link BaseSpecification} for tenant-scoped entities — those carrying {@code tenant_id}
 * (and optionally {@code branch_id}) columns: Branch, IdentityProfile, ProfileRoleAssignment,
 * RolePermissionConfig, SessionRegistry. Adds {@code tenantId}/{@code branchId} equality
 * predicates on top of the shared id / audit / soft-delete predicates.
 *
 * <p><b>G-010:</b> tenant isolation is OPT-IN — callers must apply this specification; there is
 * no automatic predicate injection until {@code insurance-security-multitenancy} is wired.
 *
 * <p>Chain tenant/branch methods <i>before</i> the inherited builders, e.g.
 * {@code new TenantScopedSpecification<X>().withTenantId(t).notDeleted()}.
 */
public class TenantScopedSpecification<T extends BaseJpaEntity> extends BaseSpecification<T> {

    private String tenantId;
    private String branchId;

    public TenantScopedSpecification<T> withTenantId(String tenantId) {
        this.tenantId = tenantId;
        return this;
    }

    public TenantScopedSpecification<T> withBranchId(String branchId) {
        this.branchId = branchId;
        return this;
    }

    @Override
    public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        List<Predicate> predicates = new ArrayList<>();

        Predicate base = super.toPredicate(root, query, cb);
        if (base != null) {
            predicates.add(base);
        }
        if (StringUtils.hasText(tenantId)) {
            predicates.add(cb.equal(root.get("tenantId"), tenantId));
        }
        if (StringUtils.hasText(branchId)) {
            predicates.add(cb.equal(root.get("branchId"), branchId));
        }
        return cb.and(predicates.toArray(new Predicate[0]));
    }
}
