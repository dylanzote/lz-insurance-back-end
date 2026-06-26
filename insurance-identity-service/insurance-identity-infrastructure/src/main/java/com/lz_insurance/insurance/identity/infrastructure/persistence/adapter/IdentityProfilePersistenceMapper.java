package com.lz_insurance.insurance.identity.infrastructure.persistence.adapter;

import com.lz_Insurance.insurance.identity.domain.model.IdentityProfile;
import com.lz_insurance.insurance.identity.infrastructure.persistence.entity.IdentityProfileEntity;
import org.mapstruct.Mapper;

/**
 * Maps {@link IdentityProfile} &harr; {@link IdentityProfileEntity}.
 * Entity&rarr;Domain rebuilds through {@code IdentityProfile.reconstitute(...)}, which enforces the
 * INTERNAL/EXTERNAL structural invariants and restores status + keycloakUserId.
 */
@Mapper(componentModel = "spring")
public interface IdentityProfilePersistenceMapper extends BaseFieldsMapper {

    IdentityProfileEntity toEntity(IdentityProfile domain);

    default IdentityProfile toDomain(IdentityProfileEntity entity) {
        if (entity == null) {
            return null;
        }
        IdentityProfile domain = IdentityProfile.reconstitute(
                entity.getTenantId(),
                entity.getBranchId(),
                entity.getActorType(),
                entity.getInternalUserType(),
                entity.getExternalUserType(),
                entity.getEmail(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getKeycloakUserId(),
                entity.getStatus());
        applyBaseFields(entity, domain);
        return domain;
    }
}
