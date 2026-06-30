package com.lz_insurance.insurance.identity.infrastructure.persistence.repository;

import com.lz_insurance.persistence.repository.BaseRepository;
import com.lz_insurance.insurance.identity.infrastructure.persistence.entity.SystemRoleEntity;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SystemRoleJpaRepository extends BaseRepository<SystemRoleEntity> {

    Optional<SystemRoleEntity> findByNameAndDeletedFalse(String name);
}
