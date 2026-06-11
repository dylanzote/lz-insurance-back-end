package com.lz_Insurance.persistence.repository;

import com.lz_Insurance.core.model.BaseDomainEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface BaseRepository<T extends BaseDomainEntity> extends JpaRepository<T, String>, JpaSpecificationExecutor<T> {

    Optional<T> findByIdAndDeletedFalse(String id);

    boolean existsByIdAndDeletedFalse(String id);

    Optional<T> findByIdAndDeletedFalseAndCreatedBy(String id, String createdBy);
}
