package com.lz_insurance.insurance.identity.infrastructure.persistence.repository;

import com.lz_Insurance.persistence.repository.BaseRepository;
import com.lz_insurance.insurance.identity.infrastructure.persistence.entity.RolePermissionEntity;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RolePermissionJpaRepository extends BaseRepository<RolePermissionEntity> {

    List<RolePermissionEntity> findByRoleIdAndDeletedFalse(String roleId);
}
