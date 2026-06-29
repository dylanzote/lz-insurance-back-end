package com.lz_insurance.insurance.identity.infrastructure.persistence.entity;

import com.lz_Insurance.persistence.entity.BaseJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** Persistence view of {@code ProfileRoleAssignment}. Data container only — no business logic. */
@Entity
@Table(name = "profile_role_assignments")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class ProfileRoleAssignmentEntity extends BaseJpaEntity {

    @Column(name = "identity_profile_id", nullable = false, length = 36)
    private String identityProfileId;

    @Column(name = "role_id", nullable = false, length = 36)
    private String roleId;

    @Column(name = "tenant_id", nullable = false, length = 36)
    private String tenantId;

    @Column(name = "branch_id", length = 36)
    private String branchId;
}
