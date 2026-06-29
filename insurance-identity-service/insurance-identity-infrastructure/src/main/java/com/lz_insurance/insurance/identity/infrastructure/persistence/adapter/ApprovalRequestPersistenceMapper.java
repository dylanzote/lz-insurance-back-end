package com.lz_insurance.insurance.identity.infrastructure.persistence.adapter;

import com.lz_Insurance.insurance.identity.domain.model.ApprovalRequest;
import com.lz_insurance.insurance.identity.infrastructure.persistence.entity.ApprovalRequestEntity;
import org.mapstruct.Mapper;

/**
 * Maps {@link ApprovalRequest} &harr; {@link ApprovalRequestEntity}.
 * Entity&rarr;Domain rebuilds through {@code ApprovalRequest.reconstitute(...)}, preserving the
 * historical {@code requestedAt} and the decided status/reviewer fields.
 */
@Mapper(componentModel = "spring")
public interface ApprovalRequestPersistenceMapper extends BaseFieldsMapper {

    ApprovalRequestEntity toEntity(ApprovalRequest domain);

    default ApprovalRequest toDomain(ApprovalRequestEntity entity) {
        if (entity == null) {
            return null;
        }
        ApprovalRequest domain = ApprovalRequest.reconstitute(
                entity.getIdentityProfileId(),
                entity.getRequestedById(),
                entity.getRequestNotes(),
                entity.getRequestedAt(),
                entity.getStatus(),
                entity.getReviewedById(),
                entity.getReviewNotes(),
                entity.getReviewedAt());
        applyBaseFields(entity, domain);
        return domain;
    }
}
