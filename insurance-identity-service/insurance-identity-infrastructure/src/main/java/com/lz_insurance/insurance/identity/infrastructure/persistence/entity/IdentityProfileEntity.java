package com.lz_insurance.insurance.identity.infrastructure.persistence.entity;

import com.lz_insurance.insurance.identity.domain.enumeration.ActorType;
import com.lz_insurance.insurance.identity.domain.enumeration.ExternalUserType;
import com.lz_insurance.insurance.identity.domain.enumeration.IdentityStatus;
import com.lz_insurance.insurance.identity.domain.enumeration.InternalUserType;
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

/** Persistence view of {@code IdentityProfile}. Data container only — no business logic. */
@Entity
@Table(name = "identity_profiles")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class IdentityProfileEntity extends BaseJpaEntity {

    @Column(name = "tenant_id", nullable = false, length = 36)
    private String tenantId;

    @Column(name = "branch_id", length = 36)
    private String branchId;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", nullable = false, length = 16)
    private ActorType actorType;

    @Enumerated(EnumType.STRING)
    @Column(name = "internal_user_type", length = 32)
    private InternalUserType internalUserType;

    @Enumerated(EnumType.STRING)
    @Column(name = "external_user_type", length = 32)
    private ExternalUserType externalUserType;

    @Column(name = "keycloak_user_id")
    private String keycloakUserId;

    @Column(name = "email", nullable = false, length = 320)
    private String email;

    @Column(name = "first_name", nullable = false, length = 128)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 128)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private IdentityStatus status;

    @Column(name = "password_change_required", nullable = false)
    private boolean passwordChangeRequired;
}
