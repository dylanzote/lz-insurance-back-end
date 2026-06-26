package com.lz_insurance.insurance.identity.infrastructure.persistence.entity;

import com.lz_Insurance.insurance.identity.domain.enumeration.TenantStatus;
import com.lz_Insurance.persistence.entity.BaseJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** Persistence view of {@code Tenant}. Data container only — no business logic. */
@Entity
@Table(name = "tenants")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class TenantEntity extends BaseJpaEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "code", nullable = false, length = 64)
    private String code;

    @Column(name = "country", nullable = false, length = 2)
    private String country;

    @Column(name = "bootstrapped", nullable = false)
    private boolean bootstrapped;

    @Column(name = "headquarter_branch_id", length = 36)
    private String headquarterBranchId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private TenantStatus status;
}
