package com.lz_insurance.insurance.identity.infrastructure.persistence.adapter;

import com.lz_insurance.insurance.identity.domain.model.RolePermissionConfig;
import com.lz_insurance.insurance.identity.infrastructure.persistence.entity.RolePermissionConfigEntity;
import org.mapstruct.Mapper;

/** Maps {@link RolePermissionConfig} &harr; {@link RolePermissionConfigEntity} (tenant overrides). */
@Mapper(componentModel = "spring")
public interface RolePermissionConfigPersistenceMapper extends BaseFieldsMapper {

    RolePermissionConfigEntity toEntity(RolePermissionConfig domain);

    default RolePermissionConfig toDomain(RolePermissionConfigEntity entity) {
        if (entity == null) {
            return null;
        }
        RolePermissionConfig domain = RolePermissionConfig.reconstitute(
                entity.getTenantId(),
                entity.getRoleId(),
                entity.getPermissionId(),
                entity.getGrantedScope());
        applyBaseFields(entity, domain);
        return domain;
    }
}
