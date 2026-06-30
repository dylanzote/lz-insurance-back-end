package com.lz_insurance.insurance.identity.infrastructure.persistence.repository;

import com.lz_insurance.persistence.repository.BaseRepository;
import com.lz_insurance.insurance.identity.infrastructure.persistence.entity.TenantEntity;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data repository for {@link TenantEntity}. Inherits CRUD, Specification execution
 * and the shared soft-delete finders from {@link BaseRepository} — only the tenant-specific
 * business-key lookup is declared here.
 */
@Repository
public interface TenantJpaRepository extends BaseRepository<TenantEntity> {

    Optional<TenantEntity> findByCodeAndDeletedFalse(String code);
}
