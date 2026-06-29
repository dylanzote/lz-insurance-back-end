package com.lz_insurance.insurance.identity.infrastructure.persistence.repository;

import com.lz_Insurance.persistence.repository.BaseRepository;
import com.lz_insurance.insurance.identity.infrastructure.persistence.entity.RolePermissionConfigEntity;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RolePermissionConfigJpaRepository extends BaseRepository<RolePermissionConfigEntity> {

    List<RolePermissionConfigEntity> findByTenantIdAndRoleIdAndDeletedFalse(String tenantId, String roleId);
}
