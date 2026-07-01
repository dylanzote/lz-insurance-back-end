package com.lz_insurance.insurance.identity.infrastructure.persistence.adapter;

import com.lz_insurance.insurance.identity.domain.enums.RoleType;
import com.lz_insurance.insurance.identity.domain.model.SystemRole;
import com.lz_insurance.insurance.identity.infrastructure.persistence.IdentityPersistenceIT;
import com.lz_insurance.insurance.identity.infrastructure.persistence.entity.SystemRoleEntity;
import com.lz_insurance.insurance.identity.infrastructure.persistence.repository.SystemRoleJpaRepository;
import com.lz_insurance.insurance.identity.infrastructure.persistence.specification.EntitySpecification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.domain.Specification;

import static org.assertj.core.api.Assertions.assertThat;

@Import({SystemRoleRepositoryAdapter.class, SystemRolePersistenceMapperImpl.class})
@DisplayName("SystemRoleRepositoryAdapter — persistence round-trip (catalog)")
class SystemRoleRepositoryAdapterIT extends IdentityPersistenceIT {

    @Autowired
    private SystemRoleRepositoryAdapter adapter;

    @Autowired
    private SystemRoleJpaRepository jpaRepository;

    @Test
    @DisplayName("save then reload preserves id and audit fields")
    void saveThenReload_preservesIdentityAndAudit() {
        String name = "ROLE_AGENT_" + uniq();
        SystemRole saved = adapter.save(new SystemRole(name, RoleType.SYSTEM, "Agent role"));
        flushAndClear();

        assertThat(saved.getId()).isNotBlank();
        assertThat(saved.getCreatedBy()).isEqualTo("test-user");

        SystemRole loaded = adapter.findById(saved.getId()).orElseThrow();
        assertThat(loaded.getName()).isEqualTo(name);
        assertThat(loaded.getRoleType()).isEqualTo(RoleType.SYSTEM);
        assertThat(loaded.getVersion()).isZero();
    }

    @Test
    @DisplayName("findByName returns the matching role")
    void findByName_returnsMatch() {
        String name = "ROLE_UNDERWRITER_" + uniq();
        adapter.save(new SystemRole(name, RoleType.SYSTEM, "Underwriter role"));
        flushAndClear();

        assertThat(adapter.findByName(name)).isPresent();
        assertThat(adapter.findByName("ROLE_UNKNOWN")).isEmpty();
    }

    @Test
    @DisplayName("BaseSpecification filters by createdBy")
    void specification_filtersByCreatedBy() {
        adapter.save(new SystemRole("ROLE_" + uniq(), RoleType.SYSTEM, "role"));
        flushAndClear();

        Specification<SystemRoleEntity> mine =
                new EntitySpecification<SystemRoleEntity>().notDeleted().withCreatedBy("test-user");
        assertThat(jpaRepository.findAll(mine)).isNotEmpty();

        Specification<SystemRoleEntity> others =
                new EntitySpecification<SystemRoleEntity>().notDeleted().withCreatedBy("someone-else");
        assertThat(jpaRepository.findAll(others)).isEmpty();
    }
}
