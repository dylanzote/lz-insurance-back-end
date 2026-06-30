package com.lz_insurance.insurance.identity.infrastructure.persistence.adapter;

import com.lz_insurance.insurance.identity.domain.model.RolePermission;
import com.lz_insurance.insurance.identity.infrastructure.persistence.entity.RolePermissionEntity;
import org.mapstruct.Mapper;

/** Maps {@link RolePermission} &harr; {@link RolePermissionEntity} (system default grants). */
@Mapper(componentModel = "spring")
public interface RolePermissionPersistenceMapper extends BaseFieldsMapper {

    RolePermissionEntity toEntity(RolePermission domain);

    default RolePermission toDomain(RolePermissionEntity entity) {
        if (entity == null) {
            return null;
        }
        RolePermission domain = RolePermission.reconstitute(
                entity.getRoleId(),
                entity.getPermissionId(),
                entity.getGrantedScope());
        applyBaseFields(entity, domain);
        return domain;
    }
}
