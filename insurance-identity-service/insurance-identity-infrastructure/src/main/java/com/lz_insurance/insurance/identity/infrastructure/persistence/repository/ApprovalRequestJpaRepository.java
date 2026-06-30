package com.lz_insurance.insurance.identity.infrastructure.persistence.repository;

import com.lz_insurance.persistence.repository.BaseRepository;
import com.lz_insurance.insurance.identity.infrastructure.persistence.entity.ApprovalRequestEntity;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApprovalRequestJpaRepository extends BaseRepository<ApprovalRequestEntity> {

    List<ApprovalRequestEntity> findByIdentityProfileIdAndDeletedFalse(String identityProfileId);
}
