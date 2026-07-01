package com.lz_insurance.insurance.identity.infrastructure.persistence.adapter;

import com.lz_insurance.insurance.identity.domain.model.IdentityProfile;
import com.lz_insurance.insurance.identity.domain.port.out.IdentityProfileRepository;
import com.lz_insurance.insurance.identity.infrastructure.persistence.repository.IdentityProfileJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** Infrastructure adapter implementing the {@link IdentityProfileRepository} out-port. */
@Component
@RequiredArgsConstructor
public class IdentityProfileRepositoryAdapter implements IdentityProfileRepository {

    private final IdentityProfileJpaRepository jpaRepository;
    private final IdentityProfilePersistenceMapper mapper;

    @Override
    public IdentityProfile save(IdentityProfile profile) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(profile)));
    }

    @Override
    public Optional<IdentityProfile> findById(String id) {
        return jpaRepository.findByIdAndDeletedFalse(id).map(mapper::toDomain);
    }

    @Override
    public boolean existsById(String id) {
        return jpaRepository.existsByIdAndDeletedFalse(id);
    }

    @Override
    public Optional<IdentityProfile> findByKeycloakUserId(String keycloakUserId) {
        return jpaRepository.findByKeycloakUserIdAndDeletedFalse(keycloakUserId).map(mapper::toDomain);
    }

    @Override
    public Optional<IdentityProfile> findByTenantIdAndEmail(String tenantId, String email) {
        return jpaRepository.findByTenantIdAndEmailAndDeletedFalse(tenantId, email).map(mapper::toDomain);
    }

    @Override
    public void deleteById(String id) {
        jpaRepository.deleteById(id);
    }
}
