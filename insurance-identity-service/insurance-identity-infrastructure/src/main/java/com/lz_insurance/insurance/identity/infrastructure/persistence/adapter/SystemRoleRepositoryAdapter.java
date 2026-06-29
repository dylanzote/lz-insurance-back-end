package com.lz_insurance.insurance.identity.infrastructure.persistence.adapter;

import com.lz_Insurance.insurance.identity.domain.model.SystemRole;
import com.lz_Insurance.insurance.identity.domain.port.out.SystemRoleRepository;
import com.lz_insurance.insurance.identity.infrastructure.persistence.repository.SystemRoleJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/** Infrastructure adapter implementing the {@link SystemRoleRepository} out-port. */
@Component
@RequiredArgsConstructor
public class SystemRoleRepositoryAdapter implements SystemRoleRepository {

    private final SystemRoleJpaRepository jpaRepository;
    private final SystemRolePersistenceMapper mapper;

    @Override
    public SystemRole save(SystemRole role) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(role)));
    }

    @Override
    public Optional<SystemRole> findById(String id) {
        return jpaRepository.findByIdAndDeletedFalse(id).map(mapper::toDomain);
    }

    @Override
    public boolean existsById(String id) {
        return jpaRepository.existsByIdAndDeletedFalse(id);
    }

    @Override
    public Optional<SystemRole> findByName(String name) {
        return jpaRepository.findByNameAndDeletedFalse(name).map(mapper::toDomain);
    }

    @Override
    public List<SystemRole> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }
}
