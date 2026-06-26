package com.lz_insurance.insurance.identity.infrastructure.persistence.adapter;

import com.lz_Insurance.insurance.identity.domain.model.ProfileRoleAssignment;
import com.lz_insurance.insurance.identity.infrastructure.persistence.entity.ProfileRoleAssignmentEntity;
import org.mapstruct.Mapper;

/** Maps {@link ProfileRoleAssignment} &harr; {@link ProfileRoleAssignmentEntity}. */
@Mapper(componentModel = "spring")
public interface ProfileRoleAssignmentPersistenceMapper extends BaseFieldsMapper {

    ProfileRoleAssignmentEntity toEntity(ProfileRoleAssignment domain);

    default ProfileRoleAssignment toDomain(ProfileRoleAssignmentEntity entity) {
        if (entity == null) {
            return null;
        }
        ProfileRoleAssignment domain = ProfileRoleAssignment.reconstitute(
                entity.getIdentityProfileId(),
                entity.getRoleId(),
                entity.getTenantId(),
                entity.getBranchId());
        applyBaseFields(entity, domain);
        return domain;
    }
}
