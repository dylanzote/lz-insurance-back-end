package com.lz_insurance.insurance.identity.infrastructure.persistence.adapter;

import com.lz_Insurance.insurance.identity.domain.enumeration.OrganizationScope;
import com.lz_Insurance.insurance.identity.domain.enumeration.PermissionAction;
import com.lz_Insurance.insurance.identity.domain.enumeration.PermissionResource;
import com.lz_Insurance.insurance.identity.domain.model.Permission;
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
        Permission saved = adapter.save(new Permission(PermissionResource.CLAIM, PermissionAction.APPROVE, OrganizationScope.BRANCH));
        flushAndClear();

        assertThat(saved.getId()).isNotBlank();
        assertThat(saved.getCreatedBy()).isEqualTo("test-user");

        Permission loaded = adapter.findById(saved.getId()).orElseThrow();
        assertThat(loaded.getResource()).isEqualTo(PermissionResource.CLAIM);
        assertThat(loaded.getAction()).isEqualTo(PermissionAction.APPROVE);
        assertThat(loaded.getDefaultScope()).isEqualTo(OrganizationScope.BRANCH);
        assertThat(loaded.getVersion()).isZero();
    }

    @Test
    @DisplayName("findByResourceAndAction returns the matching permission")
    void findByResourceAndAction_returnsMatch() {
        adapter.save(new Permission(PermissionResource.TENANT, PermissionAction.CONFIGURE, OrganizationScope.ALL));
        flushAndClear();

        assertThat(adapter.findByResourceAndAction(PermissionResource.TENANT, PermissionAction.CONFIGURE)).isPresent();
        assertThat(adapter.findByResourceAndAction(PermissionResource.TENANT, PermissionAction.DELETE)).isEmpty();
    }

    @Test
    @DisplayName("BaseSpecification filters by soft-delete flag")
    void specification_filtersByNotDeleted() {
        adapter.save(new Permission(PermissionResource.REPORT, PermissionAction.EXPORT, OrganizationScope.ALL));
        flushAndClear();

        Specification<PermissionEntity> active = new EntitySpecification<PermissionEntity>().notDeleted();
        assertThat(jpaRepository.findAll(active)).isNotEmpty();

        Specification<PermissionEntity> deleted = new EntitySpecification<PermissionEntity>().onlyDeleted();
        assertThat(jpaRepository.findAll(deleted)).isEmpty();
    }
}
