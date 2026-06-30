package com.lz_insurance.insurance.identity.infrastructure.persistence.adapter;

import com.lz_insurance.insurance.identity.domain.model.ProfileRoleAssignment;
import com.lz_insurance.insurance.identity.domain.port.out.ProfileRoleAssignmentRepository;
import com.lz_insurance.insurance.identity.infrastructure.persistence.repository.ProfileRoleAssignmentJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/** Infrastructure adapter implementing the {@link ProfileRoleAssignmentRepository} out-port. */
@Component
@RequiredArgsConstructor
public class ProfileRoleAssignmentRepositoryAdapter implements ProfileRoleAssignmentRepository {

    private final ProfileRoleAssignmentJpaRepository jpaRepository;
    private final ProfileRoleAssignmentPersistenceMapper mapper;

    @Override
    public ProfileRoleAssignment save(ProfileRoleAssignment assignment) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(assignment)));
    }

    @Override
    public Optional<ProfileRoleAssignment> findById(String id) {
        return jpaRepository.findByIdAndDeletedFalse(id).map(mapper::toDomain);
    }

    @Override
    public boolean existsById(String id) {
        return jpaRepository.existsByIdAndDeletedFalse(id);
    }

    @Override
    public List<ProfileRoleAssignment> findByIdentityProfileId(String identityProfileId) {
        return jpaRepository.findByIdentityProfileIdAndDeletedFalse(identityProfileId)
                .stream().map(mapper::toDomain).toList();
    }
}
