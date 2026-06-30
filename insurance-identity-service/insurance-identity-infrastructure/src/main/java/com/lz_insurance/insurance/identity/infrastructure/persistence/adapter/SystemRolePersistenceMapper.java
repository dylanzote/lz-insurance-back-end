package com.lz_insurance.insurance.identity.infrastructure.persistence.adapter;

import com.lz_insurance.insurance.identity.domain.model.SystemRole;
import com.lz_insurance.insurance.identity.infrastructure.persistence.entity.SystemRoleEntity;
import org.mapstruct.Mapper;

/** Maps {@link SystemRole} &harr; {@link SystemRoleEntity}. */
@Mapper(componentModel = "spring")
public interface SystemRolePersistenceMapper extends BaseFieldsMapper {

    SystemRoleEntity toEntity(SystemRole domain);

    default SystemRole toDomain(SystemRoleEntity entity) {
        if (entity == null) {
            return null;
        }
        SystemRole domain = SystemRole.reconstitute(
                entity.getName(),
                entity.getRoleType(),
                entity.getDescription());
        applyBaseFields(entity, domain);
        return domain;
    }
}
