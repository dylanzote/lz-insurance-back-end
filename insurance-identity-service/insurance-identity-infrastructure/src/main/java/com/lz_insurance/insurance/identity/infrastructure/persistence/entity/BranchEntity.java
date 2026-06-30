package com.lz_insurance.insurance.identity.infrastructure.persistence.entity;

import com.lz_insurance.insurance.identity.domain.enumeration.BranchStatus;
import com.lz_insurance.insurance.identity.domain.enumeration.BranchType;
import com.lz_insurance.persistence.entity.BaseJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** Persistence view of {@code Branch}. Data container only — no business logic. */
@Entity
@Table(name = "branches")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class BranchEntity extends BaseJpaEntity {

    @Column(name = "tenant_id", nullable = false, length = 36)
    private String tenantId;

    @Column(name = "parent_branch_id", length = 36)
    private String parentBranchId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "code", nullable = false, length = 64)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 32)
    private BranchType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private BranchStatus status;
}
