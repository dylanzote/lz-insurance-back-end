package com.lz_insurance.insurance.identity.infrastructure.persistence.adapter;

import com.lz_insurance.insurance.identity.domain.enumeration.ApprovalStatus;
import com.lz_insurance.insurance.identity.domain.model.ApprovalRequest;
import com.lz_insurance.insurance.identity.infrastructure.persistence.IdentityPersistenceIT;
import com.lz_insurance.insurance.identity.infrastructure.persistence.entity.ApprovalRequestEntity;
import com.lz_insurance.insurance.identity.infrastructure.persistence.repository.ApprovalRequestJpaRepository;
import com.lz_insurance.insurance.identity.infrastructure.persistence.specification.EntitySpecification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.domain.Specification;

import static org.assertj.core.api.Assertions.assertThat;

@Import({ApprovalRequestRepositoryAdapter.class, ApprovalRequestPersistenceMapperImpl.class})
@DisplayName("ApprovalRequestRepositoryAdapter — persistence round-trip")
class ApprovalRequestRepositoryAdapterIT extends IdentityPersistenceIT {

    @Autowired
    private ApprovalRequestRepositoryAdapter adapter;

    @Autowired
    private ApprovalRequestJpaRepository jpaRepository;

    @Test
    @DisplayName("save then reload preserves id and starts PENDING")
    void saveThenReload_preservesIdentityAndAudit() {
        String tenantId = persistTenant();
        String branchId = persistBranch(tenantId);
        String subjectId = persistProfile(tenantId, branchId);
        String requesterId = persistProfile(tenantId, branchId);

        ApprovalRequest saved = adapter.save(new ApprovalRequest(subjectId, requesterId, "Please approve"));
        flushAndClear();

        assertThat(saved.getId()).isNotBlank();
        assertThat(saved.getCreatedBy()).isEqualTo("test-user");

        ApprovalRequest loaded = adapter.findById(saved.getId()).orElseThrow();
        assertThat(loaded.getIdentityProfileId()).isEqualTo(subjectId);
        assertThat(loaded.getRequestedById()).isEqualTo(requesterId);
        assertThat(loaded.getStatus()).isEqualTo(ApprovalStatus.PENDING);
        assertThat(loaded.getRequestedAt()).isNotNull();
        assertThat(loaded.getVersion()).isZero();
    }

    @Test
    @DisplayName("approval increments version and records the decision")
    void approve_incrementsVersionAndRecordsDecision() {
        String tenantId = persistTenant();
        String branchId = persistBranch(tenantId);
        String subjectId = persistProfile(tenantId, branchId);
        String requesterId = persistProfile(tenantId, branchId);
        String reviewerId = persistProfile(tenantId, branchId);

        ApprovalRequest saved = adapter.save(new ApprovalRequest(subjectId, requesterId, "Please approve"));
        flushAndClear();

        ApprovalRequest loaded = adapter.findById(saved.getId()).orElseThrow();
        loaded.approve(reviewerId, "Looks good");
        adapter.save(loaded);
        flushAndClear();

        ApprovalRequest reloaded = adapter.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getVersion()).isEqualTo(1L);
        assertThat(reloaded.getStatus()).isEqualTo(ApprovalStatus.APPROVED);
        assertThat(reloaded.getReviewedById()).isEqualTo(reviewerId);
        assertThat(reloaded.getReviewedAt()).isNotNull();
        assertThat(reloaded.getUpdatedBy()).isEqualTo("test-user");
    }

    @Test
    @DisplayName("findByIdentityProfileId returns the subject's requests")
    void findByIdentityProfileId_returnsRequests() {
        String tenantId = persistTenant();
        String branchId = persistBranch(tenantId);
        String subjectId = persistProfile(tenantId, branchId);
        String requesterId = persistProfile(tenantId, branchId);
        adapter.save(new ApprovalRequest(subjectId, requesterId, "Please approve"));
        flushAndClear();

        assertThat(adapter.findByIdentityProfileId(subjectId)).hasSize(1);
        assertThat(adapter.findByIdentityProfileId("no-such-profile")).isEmpty();
    }

    @Test
    @DisplayName("BaseSpecification filters by createdBy")
    void specification_filtersByCreatedBy() {
        String tenantId = persistTenant();
        String branchId = persistBranch(tenantId);
        String subjectId = persistProfile(tenantId, branchId);
        String requesterId = persistProfile(tenantId, branchId);
        adapter.save(new ApprovalRequest(subjectId, requesterId, "Please approve"));
        flushAndClear();

        Specification<ApprovalRequestEntity> mine =
                new EntitySpecification<ApprovalRequestEntity>().notDeleted().withCreatedBy("test-user");
        assertThat(jpaRepository.findAll(mine)).hasSize(1);
    }
}
