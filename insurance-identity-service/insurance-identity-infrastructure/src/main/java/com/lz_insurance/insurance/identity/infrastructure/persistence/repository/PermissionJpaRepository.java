package com.lz_insurance.insurance.identity.infrastructure.persistence.repository;

import com.lz_insurance.insurance.identity.domain.enums.PermissionAction;
import com.lz_insurance.insurance.identity.domain.enums.PermissionResource;
import com.lz_insurance.persistence.repository.BaseRepository;
import com.lz_insurance.insurance.identity.infrastructure.persistence.entity.PermissionEntity;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PermissionJpaRepository extends BaseRepository<PermissionEntity> {

    Optional<PermissionEntity> findByResourceAndActionAndDeletedFalse(PermissionResource resource,
                                                                      PermissionAction action);
}
