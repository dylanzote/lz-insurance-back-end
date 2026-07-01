package com.lz_insurance.insurance.identity.infrastructure.persistence.adapter;

import com.lz_insurance.insurance.identity.domain.enums.OrganizationScope;
import com.lz_insurance.insurance.identity.domain.enums.PermissionAction;
import com.lz_insurance.insurance.identity.domain.enums.PermissionResource;
import com.lz_insurance.insurance.identity.domain.model.Permission;
import com.lz_insurance.insurance.identity.infrastructure.persistence.IdentityPersistenceIT;
import com.lz_insurance.insurance.identity.infrastructure.persistence.entity.PermissionEntity;
import com.lz_insurance.insurance.identity.infrastructure.persistence.repository.PermissionJpaRepository;
import com.lz_insurance.insurance.identity.infrastructure.persistence.specification.EntitySpecification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.domain.Specification;

import static org.assertj.core.api.Assertions.assertThat;

@Import({PermissionRepositoryAdapter.class, PermissionPersistenceMapperImpl.class})
@DisplayName("PermissionRepositoryAdapter — persistence round-trip (catalog)")
class PermissionRepositoryAdapterIT extends IdentityPersistenceIT {

    @Autowired
    private PermissionRepositoryAdapter adapter;

    @Autowired
    private PermissionJpaRepository jpaRepository;

    @Test
    @DisplayName("save then reload preserves id, natural key and audit fields")
    void saveThenReload_preservesIdentityAndAudit() {
        // CLAIM/DELETE is intentionally NOT in the seed (011-seed-permissions seeds CLAIM
        // create/read/update/approve/reject) so this creation test never collides with seeded rows.
        assertThat(adapter.findByResourceAndAction(PermissionResource.CLAIM, PermissionAction.DELETE))
                .as("CLAIM/DELETE must not be seeded; a future seed addition would break this creation test")
                .isEmpty();

        Permission saved = adapter.save(new Permission(PermissionResource.CLAIM, PermissionAction.DELETE, OrganizationScope.BRANCH));
        flushAndClear();

        assertThat(saved.getId()).isNotBlank();
        assertThat(saved.getCreatedBy()).isEqualTo("test-user");

        Permission loaded = adapter.findById(saved.getId()).orElseThrow();
        assertThat(loaded.getResource()).isEqualTo(PermissionResource.CLAIM);
        assertThat(loaded.getAction()).isEqualTo(PermissionAction.DELETE);
        assertThat(loaded.getDefaultScope()).isEqualTo(OrganizationScope.BRANCH);
        assertThat(loaded.getVersion()).isZero();
    }

    @Test
    @DisplayName("findByResourceAndAction returns the matching permission")
    void findByResourceAndAction_returnsMatch() {
        // AUDIT/CONFIGURE and AUDIT/DELETE are both intentionally absent from the seed
        // (011-seed-permissions seeds AUDIT read/export only).
        assertThat(adapter.findByResourceAndAction(PermissionResource.AUDIT, PermissionAction.CONFIGURE))
                .as("AUDIT/CONFIGURE must not be seeded; a future seed addition would break this creation test")
                .isEmpty();

        adapter.save(new Permission(PermissionResource.AUDIT, PermissionAction.CONFIGURE, OrganizationScope.ALL));
        flushAndClear();

        assertThat(adapter.findByResourceAndAction(PermissionResource.AUDIT, PermissionAction.CONFIGURE)).isPresent();
        assertThat(adapter.findByResourceAndAction(PermissionResource.AUDIT, PermissionAction.DELETE)).isEmpty();
    }

    @Test
    @DisplayName("BaseSpecification filters by soft-delete flag")
    void specification_filtersByNotDeleted() {
        // REPORT/CREATE is intentionally NOT in the seed (011-seed-permissions seeds REPORT read/export).
        assertThat(adapter.findByResourceAndAction(PermissionResource.REPORT, PermissionAction.CREATE))
                .as("REPORT/CREATE must not be seeded; a future seed addition would break this creation test")
                .isEmpty();

        adapter.save(new Permission(PermissionResource.REPORT, PermissionAction.CREATE, OrganizationScope.ALL));
        flushAndClear();

        Specification<PermissionEntity> active = new EntitySpecification<PermissionEntity>().notDeleted();
        assertThat(jpaRepository.findAll(active)).isNotEmpty();

        Specification<PermissionEntity> deleted = new EntitySpecification<PermissionEntity>().onlyDeleted();
        assertThat(jpaRepository.findAll(deleted)).isEmpty();
    }
}
