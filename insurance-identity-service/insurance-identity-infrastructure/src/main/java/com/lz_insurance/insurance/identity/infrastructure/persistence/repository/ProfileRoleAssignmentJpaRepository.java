package com.lz_insurance.insurance.identity.infrastructure.persistence.repository;

import com.lz_Insurance.persistence.repository.BaseRepository;
import com.lz_insurance.insurance.identity.infrastructure.persistence.entity.ProfileRoleAssignmentEntity;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProfileRoleAssignmentJpaRepository extends BaseRepository<ProfileRoleAssignmentEntity> {

    List<ProfileRoleAssignmentEntity> findByIdentityProfileIdAndDeletedFalse(String identityProfileId);
}
