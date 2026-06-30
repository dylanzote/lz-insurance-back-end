package com.lz_insurance.insurance.identity.infrastructure.persistence.adapter;

import com.lz_insurance.insurance.identity.domain.enumeration.TenantStatus;
import com.lz_insurance.insurance.identity.domain.model.Tenant;
import com.lz_insurance.insurance.identity.infrastructure.persistence.IdentityPersistenceIT;
import com.lz_insurance.insurance.identity.infrastructure.persistence.entity.TenantEntity;
import com.lz_insurance.insurance.identity.infrastructure.persistence.repository.TenantJpaRepository;
import com.lz_insurance.insurance.identity.infrastructure.persistence.specification.EntitySpecification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for {@link TenantRepositoryAdapter} against a real Postgres. Proves the
 * full round-trip: domain {@code ->} entity {@code ->} stored {@code ->} reconstituted domain,
 * id preserved, audit fields populated by JPA auditing, version managed by {@code @Version},
 * plus a {@code BaseSpecification} query through the adapter's repository.
 */
@Import({TenantRepositoryAdapter.class, TenantPersistenceMapperImpl.class})
@DisplayName("TenantRepositoryAdapter — persistence round-trip")
class TenantRepositoryAdapterIT extends IdentityPersistenceIT {

    @Autowired
    private TenantRepositoryAdapter adapter;

    @Autowired
    private TenantJpaRepository jpaRepository;

    @Test
    @DisplayName("save then reload preserves id and populates audit fields")
    void saveThenReload_preservesIdentityAndAudit() {
        Tenant saved = adapter.save(new Tenant("Acme Insurance", "ACME", "ca"));
        flushAndClear();

        assertThat(saved.getId()).isNotBlank();
        assertThat(saved.getCreatedBy()).isEqualTo("test-user");
        assertThat(saved.getCreatedAt()).isNotNull();

        Tenant loaded = adapter.findById(saved.getId()).orElseThrow();
        assertThat(loaded.getId()).isEqualTo(saved.getId());
        assertThat(loaded.getCode()).isEqualTo("ACME");
        assertThat(loaded.getCountry()).isEqualTo("CA");
        assertThat(loaded.getStatus()).isEqualTo(TenantStatus.ACTIVE);
        assertThat(loaded.isBootstrapped()).isFalse();
        assertThat(loaded.getVersion()).isZero();
    }

    @Test
    @DisplayName("update increments version and records the modifier")
    void update_incrementsVersionAndRecordsModifier() {
        Tenant saved = adapter.save(new Tenant("Globex", "GLOBEX", "CA"));
        flushAndClear();

        Tenant loaded = adapter.findById(saved.getId()).orElseThrow();
        loaded.markBootstrapped("branch-hq-0000000000001");
        adapter.save(loaded);
        flushAndClear();

        Tenant reloaded = adapter.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getVersion()).isEqualTo(1L);
        assertThat(reloaded.isBootstrapped()).isTrue();
        assertThat(reloaded.getHeadquarterBranchId()).isEqualTo("branch-hq-0000000000001");
        assertThat(reloaded.getUpdatedBy()).isEqualTo("test-user");
        assertThat(reloaded.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("findByCode returns only the matching non-deleted tenant")
    void findByCode_returnsMatch() {
        adapter.save(new Tenant("Acme Insurance", "ACME", "CA"));
        adapter.save(new Tenant("Globex", "GLOBEX", "CA"));
        flushAndClear();

        assertThat(adapter.findByCode("ACME"))
                .get().extracting(Tenant::getName).isEqualTo("Acme Insurance");
        assertThat(adapter.findByCode("UNKNOWN")).isEmpty();
    }

    @Test
    @DisplayName("BaseSpecification filters by soft-delete flag and createdBy")
    void specification_filtersByAuditFields() {
        adapter.save(new Tenant("Acme Insurance", "ACME", "CA"));
        adapter.save(new Tenant("Globex", "GLOBEX", "CA"));
        flushAndClear();

        Specification<TenantEntity> mine =
                new EntitySpecification<TenantEntity>().notDeleted().withCreatedBy("test-user");
        List<TenantEntity> result = jpaRepository.findAll(mine);
        assertThat(result).extracting(TenantEntity::getCode)
                .containsExactlyInAnyOrder("ACME", "GLOBEX");

        Specification<TenantEntity> others =
                new EntitySpecification<TenantEntity>().notDeleted().withCreatedBy("someone-else");
        assertThat(jpaRepository.findAll(others)).isEmpty();
    }
}
