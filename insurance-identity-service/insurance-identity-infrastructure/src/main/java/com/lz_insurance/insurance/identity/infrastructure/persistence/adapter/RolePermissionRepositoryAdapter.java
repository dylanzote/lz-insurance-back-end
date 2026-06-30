package com.lz_insurance.insurance.identity.infrastructure.persistence.adapter;

import com.lz_insurance.insurance.identity.domain.model.RolePermission;
import com.lz_insurance.insurance.identity.domain.port.out.RolePermissionRepository;
import com.lz_insurance.insurance.identity.infrastructure.persistence.repository.RolePermissionJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/** Infrastructure adapter implementing the {@link RolePermissionRepository} out-port. */
@Component
@RequiredArgsConstructor
public class RolePermissionRepositoryAdapter implements RolePermissionRepository {

    private final RolePermissionJpaRepository jpaRepository;
    private final RolePermissionPersistenceMapper mapper;

    @Override
    public RolePermission save(RolePermission rolePermission) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(rolePermission)));
    }

    @Override
    public Optional<RolePermission> findById(String id) {
        return jpaRepository.findByIdAndDeletedFalse(id).map(mapper::toDomain);
    }

    @Override
    public boolean existsById(String id) {
        return jpaRepository.existsByIdAndDeletedFalse(id);
    }

    @Override
    public List<RolePermission> findByRoleId(String roleId) {
        return jpaRepository.findByRoleIdAndDeletedFalse(roleId).stream().map(mapper::toDomain).toList();
    }
}
