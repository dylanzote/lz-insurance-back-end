package com.lz_insurance.insurance.identity.infrastructure.persistence.entity;

import com.lz_insurance.insurance.identity.domain.enums.SessionStatus;
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

import java.time.LocalDateTime;

/** Persistence view of {@code SessionRegistry}. Data container only — no business logic. */
@Entity
@Table(name = "session_registry")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class SessionRegistryEntity extends BaseJpaEntity {

    @Column(name = "identity_profile_id", nullable = false, length = 36)
    private String identityProfileId;

    @Column(name = "tenant_id", nullable = false, length = 36)
    private String tenantId;

    @Column(name = "session_token", nullable = false, length = 512)
    private String sessionToken;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private SessionStatus status;

    @Column(name = "last_activity_at")
    private LocalDateTime lastActivityAt;
}
