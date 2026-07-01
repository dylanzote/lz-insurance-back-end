package com.lz_insurance.insurance.identity.infrastructure.persistence.entity;

import com.lz_insurance.insurance.identity.domain.enums.ApprovalStatus;
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

/** Persistence view of {@code ApprovalRequest}. Data container only — no business logic. */
@Entity
@Table(name = "approval_requests")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class ApprovalRequestEntity extends BaseJpaEntity {

    @Column(name = "identity_profile_id", nullable = false, length = 36)
    private String identityProfileId;

    @Column(name = "requested_by_id", nullable = false, length = 36)
    private String requestedById;

    @Column(name = "request_notes", length = 1024)
    private String requestNotes;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "reviewed_by_id", length = 36)
    private String reviewedById;

    @Column(name = "review_notes", length = 1024)
    private String reviewNotes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private ApprovalStatus status;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;
}
