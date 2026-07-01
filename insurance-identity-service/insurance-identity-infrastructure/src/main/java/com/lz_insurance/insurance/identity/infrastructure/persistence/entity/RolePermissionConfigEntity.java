package com.lz_insurance.insurance.identity.infrastructure.persistence.entity;

import com.lz_insurance.insurance.identity.domain.enums.OrganizationScope;
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

/** Persistence view of {@code RolePermissionConfig}. Data container only — no business logic. */
@Entity
@Table(name = "role_permission_configs")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class RolePermissionConfigEntity extends BaseJpaEntity {

    @Column(name = "tenant_id", nullable = false, length = 36)
    private String tenantId;

    @Column(name = "role_id", nullable = false, length = 36)
    private String roleId;

    @Column(name = "permission_id", nullable = false, length = 36)
    private String permissionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "granted_scope", nullable = false, length = 16)
    private OrganizationScope grantedScope;
}
