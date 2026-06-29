package com.lz_insurance.insurance.identity.infrastructure.persistence.adapter;

import com.lz_Insurance.insurance.identity.domain.model.RolePermissionConfig;
import com.lz_Insurance.insurance.identity.domain.port.out.RolePermissionConfigRepository;
import com.lz_insurance.insurance.identity.infrastructure.persistence.repository.RolePermissionConfigJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/** Infrastructure adapter implementing the {@link RolePermissionConfigRepository} out-port. */
@Component
@RequiredArgsConstructor
public class RolePermissionConfigRepositoryAdapter implements RolePermissionConfigRepository {

    private final RolePermissionConfigJpaRepository jpaRepository;
    private final RolePermissionConfigPersistenceMapper mapper;

    @Override
    public RolePermissionConfig save(RolePermissionConfig config) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(config)));
    }

    @Override
    public Optional<RolePermissionConfig> findById(String id) {
        return jpaRepository.findByIdAndDeletedFalse(id).map(mapper::toDomain);
    }

    @Override
    public boolean existsById(String id) {
        return jpaRepository.existsByIdAndDeletedFalse(id);
    }

    @Override
    public List<RolePermissionConfig> findByTenantIdAndRoleId(String tenantId, String roleId) {
        return jpaRepository.findByTenantIdAndRoleIdAndDeletedFalse(tenantId, roleId)
                .stream().map(mapper::toDomain).toList();
    }
}
