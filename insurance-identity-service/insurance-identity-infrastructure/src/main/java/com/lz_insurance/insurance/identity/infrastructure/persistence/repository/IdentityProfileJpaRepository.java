package com.lz_insurance.insurance.identity.infrastructure.persistence.repository;

import com.lz_Insurance.persistence.repository.BaseRepository;
import com.lz_insurance.insurance.identity.infrastructure.persistence.entity.IdentityProfileEntity;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IdentityProfileJpaRepository extends BaseRepository<IdentityProfileEntity> {

    Optional<IdentityProfileEntity> findByKeycloakUserIdAndDeletedFalse(String keycloakUserId);

    Optional<IdentityProfileEntity> findByTenantIdAndEmailAndDeletedFalse(String tenantId, String email);
}
