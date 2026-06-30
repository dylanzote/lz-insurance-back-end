package com.lz_insurance.insurance.identity.infrastructure.persistence.entity;

import com.lz_insurance.insurance.identity.domain.enumeration.RoleType;
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

/** Persistence view of {@code SystemRole}. Data container only — no business logic. */
@Entity
@Table(name = "system_roles")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class SystemRoleEntity extends BaseJpaEntity {

    @Column(name = "name", nullable = false, length = 64)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_type", nullable = false, length = 16)
    private RoleType roleType;

    @Column(name = "description", length = 512)
    private String description;
}
