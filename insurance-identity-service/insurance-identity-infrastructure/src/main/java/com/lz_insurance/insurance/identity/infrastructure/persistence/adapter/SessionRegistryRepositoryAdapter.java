package com.lz_insurance.insurance.identity.infrastructure.persistence.adapter;

import com.lz_insurance.insurance.identity.domain.model.SessionRegistry;
import com.lz_insurance.insurance.identity.domain.port.out.SessionRegistryRepository;
import com.lz_insurance.insurance.identity.infrastructure.persistence.repository.SessionRegistryJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** Infrastructure adapter implementing the {@link SessionRegistryRepository} out-port. */
@Component
@RequiredArgsConstructor
public class SessionRegistryRepositoryAdapter implements SessionRegistryRepository {

    private final SessionRegistryJpaRepository jpaRepository;
    private final SessionRegistryPersistenceMapper mapper;

    @Override
    public SessionRegistry save(SessionRegistry session) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(session)));
    }

    @Override
    public Optional<SessionRegistry> findById(String id) {
        return jpaRepository.findByIdAndDeletedFalse(id).map(mapper::toDomain);
    }

    @Override
    public boolean existsById(String id) {
        return jpaRepository.existsByIdAndDeletedFalse(id);
    }

    @Override
    public Optional<SessionRegistry> findBySessionToken(String sessionToken) {
        return jpaRepository.findBySessionTokenAndDeletedFalse(sessionToken).map(mapper::toDomain);
    }
}
