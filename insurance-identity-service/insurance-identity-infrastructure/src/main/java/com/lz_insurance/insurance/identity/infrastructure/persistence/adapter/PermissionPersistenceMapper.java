package com.lz_insurance.insurance.identity.infrastructure.persistence.adapter;

import com.lz_Insurance.insurance.identity.domain.model.Permission;
import com.lz_insurance.insurance.identity.infrastructure.persistence.entity.PermissionEntity;
import org.mapstruct.Mapper;

/** Maps {@link Permission} &harr; {@link PermissionEntity}. */
@Mapper(componentModel = "spring")
public interface PermissionPersistenceMapper extends BaseFieldsMapper {

    PermissionEntity toEntity(Permission domain);

    default Permission toDomain(PermissionEntity entity) {
        if (entity == null) {
            return null;
        }
        Permission domain = Permission.reconstitute(
                entity.getResource(),
                entity.getAction(),
                entity.getDefaultScope());
        applyBaseFields(entity, domain);
        return domain;
    }
}
