package com.lz_insurance.insurance.identity.infrastructure.persistence.repository;

import com.lz_insurance.persistence.repository.BaseRepository;
import com.lz_insurance.insurance.identity.infrastructure.persistence.entity.SessionRegistryEntity;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SessionRegistryJpaRepository extends BaseRepository<SessionRegistryEntity> {

    Optional<SessionRegistryEntity> findBySessionTokenAndDeletedFalse(String sessionToken);
}
