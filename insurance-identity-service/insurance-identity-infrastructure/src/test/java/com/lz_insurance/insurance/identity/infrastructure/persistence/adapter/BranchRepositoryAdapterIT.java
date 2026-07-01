package com.lz_insurance.insurance.identity.infrastructure.persistence.adapter;

import com.lz_insurance.insurance.identity.domain.enums.BranchStatus;
import com.lz_insurance.insurance.identity.domain.enums.BranchType;
import com.lz_insurance.insurance.identity.domain.model.Branch;
import com.lz_insurance.insurance.identity.infrastructure.persistence.IdentityPersistenceIT;
import com.lz_insurance.insurance.identity.infrastructure.persistence.entity.BranchEntity;
import com.lz_insurance.insurance.identity.infrastructure.persistence.repository.BranchJpaRepository;
import com.lz_insurance.insurance.identity.infrastructure.persistence.specification.TenantScopedSpecification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.domain.Specification;

import static org.assertj.core.api.Assertions.assertThat;

@Import({BranchRepositoryAdapter.class, BranchPersistenceMapperImpl.class})
@DisplayName("BranchRepositoryAdapter — persistence round-trip (tenant-scoped)")
class BranchRepositoryAdapterIT extends IdentityPersistenceIT {

    @Autowired
    private BranchRepositoryAdapter adapter;

    @Autowired
    private BranchJpaRepository jpaRepository;

    @Test
    @DisplayName("save then reload preserves id and populates audit fields")
    void saveThenReload_preservesIdentityAndAudit() {
        String tenantId = persistTenant();
        Branch saved = adapter.save(new Branch(tenantId, null, "Head Office", "HQ-" + uniq(), BranchType.HEADQUARTER));
        flushAndClear();

        assertThat(saved.getId()).isNotBlank();
        assertThat(saved.getCreatedBy()).isEqualTo("test-user");

        Branch loaded = adapter.findById(saved.getId()).orElseThrow();
        assertThat(loaded.getTenantId()).isEqualTo(tenantId);
        assertThat(loaded.getType()).isEqualTo(BranchType.HEADQUARTER);
        assertThat(loaded.getStatus()).isEqualTo(BranchStatus.ACTIVE);
        assertThat(loaded.isTopLevel()).isTrue();
        assertThat(loaded.getVersion()).isZero();
    }

    @Test
    @DisplayName("update increments version and records the modifier")
    void update_incrementsVersionAndRecordsModifier() {
        String tenantId = persistTenant();
        Branch saved = adapter.save(new Branch(tenantId, null, "Head Office", "HQ-" + uniq(), BranchType.HEADQUARTER));
        flushAndClear();

        Branch loaded = adapter.findById(saved.getId()).orElseThrow();
        loaded.deactivate();
        adapter.save(loaded);
        flushAndClear();

        Branch reloaded = adapter.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getVersion()).isEqualTo(1L);
        assertThat(reloaded.getStatus()).isEqualTo(BranchStatus.INACTIVE);
        assertThat(reloaded.getUpdatedBy()).isEqualTo("test-user");
    }

    @Test
    @DisplayName("findByTenantIdAndCode returns the matching branch")
    void findByTenantIdAndCode_returnsMatch() {
        String tenantId = persistTenant();
        String code = "HQ-" + uniq();
        adapter.save(new Branch(tenantId, null, "Head Office", code, BranchType.HEADQUARTER));
        flushAndClear();

        assertThat(adapter.findByTenantIdAndCode(tenantId, code)).isPresent();
        assertThat(adapter.findByTenantIdAndCode(tenantId, "MISSING")).isEmpty();
    }

    @Test
    @DisplayName("TenantScopedSpecification filters by tenant and soft-delete")
    void tenantScopedSpecification_filtersByTenant() {
        String tenantId = persistTenant();
        adapter.save(new Branch(tenantId, null, "Head Office", "HQ-" + uniq(), BranchType.HEADQUARTER));
        flushAndClear();

        Specification<BranchEntity> inTenant =
                new TenantScopedSpecification<BranchEntity>().withTenantId(tenantId).notDeleted();
        assertThat(jpaRepository.findAll(inTenant)).hasSize(1);

        Specification<BranchEntity> otherTenant =
                new TenantScopedSpecification<BranchEntity>().withTenantId("no-such-tenant").notDeleted();
        assertThat(jpaRepository.findAll(otherTenant)).isEmpty();
    }
}
