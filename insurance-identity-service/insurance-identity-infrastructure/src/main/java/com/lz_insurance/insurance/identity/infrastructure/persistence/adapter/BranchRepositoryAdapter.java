package com.lz_insurance.insurance.identity.infrastructure.persistence.adapter;

import com.lz_Insurance.insurance.identity.domain.model.Branch;
import com.lz_Insurance.insurance.identity.domain.port.out.BranchRepository;
import com.lz_insurance.insurance.identity.infrastructure.persistence.repository.BranchJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/** Infrastructure adapter implementing the {@link BranchRepository} out-port. */
@Component
@RequiredArgsConstructor
public class BranchRepositoryAdapter implements BranchRepository {

    private final BranchJpaRepository jpaRepository;
    private final BranchPersistenceMapper mapper;

    @Override
    public Branch save(Branch branch) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(branch)));
    }

    @Override
    public Optional<Branch> findById(String id) {
        return jpaRepository.findByIdAndDeletedFalse(id).map(mapper::toDomain);
    }

    @Override
    public boolean existsById(String id) {
        return jpaRepository.existsByIdAndDeletedFalse(id);
    }

    @Override
    public Optional<Branch> findByTenantIdAndCode(String tenantId, String code) {
        return jpaRepository.findByTenantIdAndCodeAndDeletedFalse(tenantId, code).map(mapper::toDomain);
    }

    @Override
    public List<Branch> findByTenantId(String tenantId) {
        return jpaRepository.findByTenantIdAndDeletedFalse(tenantId).stream().map(mapper::toDomain).toList();
    }
}
