package com.lz_insurance.insurance.identity.infrastructure.persistence.adapter;

import com.lz_insurance.insurance.identity.domain.model.SessionRegistry;
import com.lz_insurance.insurance.identity.infrastructure.persistence.entity.SessionRegistryEntity;
import org.mapstruct.Mapper;

/**
 * Maps {@link SessionRegistry} &harr; {@link SessionRegistryEntity}.
 * Entity&rarr;Domain rebuilds through {@code SessionRegistry.reconstitute(...)}, restoring the
 * stored status (e.g. REVOKED) and lastActivityAt without re-running creation-time rules.
 */
@Mapper(componentModel = "spring")
public interface SessionRegistryPersistenceMapper extends BaseFieldsMapper {

    SessionRegistryEntity toEntity(SessionRegistry domain);

    default SessionRegistry toDomain(SessionRegistryEntity entity) {
        if (entity == null) {
            return null;
        }
        SessionRegistry domain = SessionRegistry.reconstitute(
                entity.getIdentityProfileId(),
                entity.getTenantId(),
                entity.getSessionToken(),
                entity.getIpAddress(),
                entity.getUserAgent(),
                entity.getExpiresAt(),
                entity.getStatus(),
                entity.getLastActivityAt());
        applyBaseFields(entity, domain);
        return domain;
    }
}
