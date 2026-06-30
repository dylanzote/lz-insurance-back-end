package com.lz_insurance.insurance.identity.infrastructure.persistence.entity;

import com.lz_insurance.insurance.identity.domain.enumeration.OrganizationScope;
import com.lz_insurance.insurance.identity.domain.enumeration.PermissionAction;
import com.lz_insurance.insurance.identity.domain.enumeration.PermissionResource;
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

/** Persistence view of {@code Permission}. Data container only — no business logic. */
@Entity
@Table(name = "permissions")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class PermissionEntity extends BaseJpaEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "resource", nullable = false, length = 32)
    private PermissionResource resource;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 32)
    private PermissionAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "default_scope", nullable = false, length = 16)
    private OrganizationScope defaultScope;
}
