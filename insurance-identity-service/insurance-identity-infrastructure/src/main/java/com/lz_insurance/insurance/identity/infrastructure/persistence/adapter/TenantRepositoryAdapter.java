package com.lz_insurance.insurance.identity.infrastructure.persistence.adapter;

import com.lz_insurance.insurance.identity.domain.model.Tenant;
import com.lz_insurance.insurance.identity.domain.port.out.TenantRepository;
import com.lz_insurance.insurance.identity.infrastructure.persistence.entity.TenantEntity;
import com.lz_insurance.insurance.identity.infrastructure.persistence.repository.TenantJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Infrastructure adapter implementing the {@link TenantRepository} out-port. Delegates
 * to {@link TenantJpaRepository} and translates {@code TenantEntity} &harr; {@link Tenant}
 * via the existing {@link TenantPersistenceMapper}. Holds no business logic.
 */
@Component
@RequiredArgsConstructor
public class TenantRepositoryAdapter implements TenantRepository {

    private final TenantJpaRepository jpaRepository;
    private final TenantPersistenceMapper mapper;

    @Override
    public Tenant save(Tenant tenant) {
        TenantEntity saved = jpaRepository.save(mapper.toEntity(tenant));
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Tenant> findById(String id) {
        return jpaRepository.findByIdAndDeletedFalse(id).map(mapper::toDomain);
    }

    @Override
    public boolean existsById(String id) {
        return jpaRepository.existsByIdAndDeletedFalse(id);
    }

    @Override
    public Optional<Tenant> findByCode(String code) {
        return jpaRepository.findByCodeAndDeletedFalse(code).map(mapper::toDomain);
    }
}
