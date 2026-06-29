package com.lz_insurance.insurance.identity.infrastructure.persistence.adapter;

import com.lz_Insurance.core.model.BaseDomainEntity;
import com.lz_Insurance.persistence.entity.BaseJpaEntity;

/**
 * Copies the shared id/version/audit/soft-delete fields from a persisted entity onto a
 * reconstituted domain model. The {@code reconstitute(...)} factories deliberately do not
 * accept base fields (to keep their signatures focused on domain state), so the mappers
 * restore those here after rebuilding the aggregate. The Domain&rarr;Entity direction is
 * handled by MapStruct by-name, so it needs no counterpart method.
 */
public interface BaseFieldsMapper {

    default void applyBaseFields(BaseJpaEntity source, BaseDomainEntity target) {
        target.setId(source.getId());
        target.setVersion(source.getVersion());
        target.setCreatedBy(source.getCreatedBy());
        target.setCreatedAt(source.getCreatedAt());
        target.setUpdatedBy(source.getUpdatedBy());
        target.setUpdatedAt(source.getUpdatedAt());
        target.setDeleted(source.isDeleted());
        target.setDeletedAt(source.getDeletedAt());
        target.setDeletedBy(source.getDeletedBy());
    }
}
