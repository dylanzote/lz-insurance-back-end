package com.lz_insurance.insurance.identity.infrastructure.persistence.repository;

import com.lz_insurance.persistence.repository.BaseRepository;
import com.lz_insurance.insurance.identity.infrastructure.persistence.entity.BranchEntity;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BranchJpaRepository extends BaseRepository<BranchEntity> {

    Optional<BranchEntity> findByTenantIdAndCodeAndDeletedFalse(String tenantId, String code);

    List<BranchEntity> findByTenantIdAndDeletedFalse(String tenantId);
}
