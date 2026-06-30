package com.lz_insurance.insurance.identity.infrastructure.persistence.adapter;

import com.lz_insurance.insurance.identity.domain.model.ApprovalRequest;
import com.lz_insurance.insurance.identity.domain.port.out.ApprovalRequestRepository;
import com.lz_insurance.insurance.identity.infrastructure.persistence.repository.ApprovalRequestJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/** Infrastructure adapter implementing the {@link ApprovalRequestRepository} out-port. */
@Component
@RequiredArgsConstructor
public class ApprovalRequestRepositoryAdapter implements ApprovalRequestRepository {

    private final ApprovalRequestJpaRepository jpaRepository;
    private final ApprovalRequestPersistenceMapper mapper;

    @Override
    public ApprovalRequest save(ApprovalRequest request) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(request)));
    }

    @Override
    public Optional<ApprovalRequest> findById(String id) {
        return jpaRepository.findByIdAndDeletedFalse(id).map(mapper::toDomain);
    }

    @Override
    public boolean existsById(String id) {
        return jpaRepository.existsByIdAndDeletedFalse(id);
    }

    @Override
    public List<ApprovalRequest> findByIdentityProfileId(String identityProfileId) {
        return jpaRepository.findByIdentityProfileIdAndDeletedFalse(identityProfileId)
                .stream().map(mapper::toDomain).toList();
    }
}
