package com.lz_insurance.insurance.identity.infrastructure.persistence.adapter;

import com.lz_Insurance.insurance.identity.domain.enumeration.PermissionAction;
import com.lz_Insurance.insurance.identity.domain.enumeration.PermissionResource;
import com.lz_Insurance.insurance.identity.domain.model.Permission;
import com.lz_Insurance.insurance.identity.domain.port.out.PermissionRepository;
import com.lz_insurance.insurance.identity.infrastructure.persistence.repository.PermissionJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/** Infrastructure adapter implementing the {@link PermissionRepository} out-port. */
@Component
@RequiredArgsConstructor
public class PermissionRepositoryAdapter implements PermissionRepository {

    private final PermissionJpaRepository jpaRepository;
    private final PermissionPersistenceMapper mapper;

    @Override
    public Permission save(Permission permission) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(permission)));
    }

    @Override
    public Optional<Permission> findById(String id) {
        return jpaRepository.findByIdAndDeletedFalse(id).map(mapper::toDomain);
    }

    @Override
    public boolean existsById(String id) {
        return jpaRepository.existsByIdAndDeletedFalse(id);
    }

    @Override
    public Optional<Permission> findByResourceAndAction(PermissionResource resource, PermissionAction action) {
        return jpaRepository.findByResourceAndActionAndDeletedFalse(resource, action).map(mapper::toDomain);
    }

    @Override
    public List<Permission> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }
}
