package com.lz_insurance.insurance.identity.infrastructure.persistence.specification;

import com.lz_insurance.persistence.entity.BaseJpaEntity;
import com.lz_insurance.persistence.specification.BaseSpecification;

/**
 * Concrete, instantiable {@link BaseSpecification} for entities that carry no tenant scope
 * (e.g. {@code TenantEntity}, catalog tables). Exposes the shared id / audit / soft-delete
 * predicates with no additions.
 *
 * <p>For tenant-scoped entities use {@code TenantScopedSpecification} instead, which adds
 * {@code tenantId}/{@code branchId} predicates.
 */
public class EntitySpecification<T extends BaseJpaEntity> extends BaseSpecification<T> {
}
