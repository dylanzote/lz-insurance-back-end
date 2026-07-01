package com.lz_insurance.insurance.identity.domain.model;

import com.lz_insurance.core.model.BaseDomainEntity;
import com.lz_insurance.insurance.identity.domain.enums.ApprovalStatus;
import com.lz_insurance.insurance.identity.domain.exception.ApprovalAlreadyDecidedException;
import com.lz_insurance.insurance.identity.domain.support.DomainGuard;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * The approval gate that must reach APPROVED before an internal registration is
 * provisioned in Keycloak. A request is created PENDING and is decided exactly
 * once: a second {@link #approve} or {@link #reject} is rejected.
 */
@Getter
@ToString(callSuper = true)
public class ApprovalRequest extends BaseDomainEntity {

    private final String identityProfileId;
    private final String requestedById;
    private final String requestNotes;
    private final LocalDateTime requestedAt;

    private String reviewedById;
    private String reviewNotes;
    private ApprovalStatus status;
    private LocalDateTime reviewedAt;

    public ApprovalRequest(String identityProfileId, String requestedById, String requestNotes) {
        DomainGuard.notBlank(identityProfileId, "identityProfileId");
        DomainGuard.notBlank(requestedById, "requestedById");

        this.identityProfileId = identityProfileId;
        this.requestedById = requestedById;
        this.requestNotes = requestNotes;
        this.requestedAt = LocalDateTime.now();
        this.status = ApprovalStatus.PENDING;
    }

    /**
     * Reconstitution constructor for the persistence layer: restores the full decided
     * (or pending) state, including the stored {@code requestedAt}/{@code reviewedAt},
     * rather than re-stamping {@code requestedAt} to now.
     */
    private ApprovalRequest(String identityProfileId, String requestedById, String requestNotes,
                            LocalDateTime requestedAt, ApprovalStatus status,
                            String reviewedById, String reviewNotes, LocalDateTime reviewedAt) {
        DomainGuard.notBlank(identityProfileId, "identityProfileId");
        DomainGuard.notBlank(requestedById, "requestedById");
        DomainGuard.notNull(requestedAt, "requestedAt");
        DomainGuard.notNull(status, "status");

        this.identityProfileId = identityProfileId;
        this.requestedById = requestedById;
        this.requestNotes = requestNotes;
        this.requestedAt = requestedAt;
        this.status = status;
        this.reviewedById = reviewedById;
        this.reviewNotes = reviewNotes;
        this.reviewedAt = reviewedAt;
    }

    /** Rebuilds an {@code ApprovalRequest} from its persisted state (infrastructure mapper only). */
    public static ApprovalRequest reconstitute(String identityProfileId, String requestedById,
                                               String requestNotes, LocalDateTime requestedAt,
                                               ApprovalStatus status, String reviewedById,
                                               String reviewNotes, LocalDateTime reviewedAt) {
        return new ApprovalRequest(identityProfileId, requestedById, requestNotes, requestedAt,
                status, reviewedById, reviewNotes, reviewedAt);
    }

    public void approve(String reviewerId, String notes) {
        decide(ApprovalStatus.APPROVED, reviewerId, notes);
    }

    public void reject(String reviewerId, String notes) {
        decide(ApprovalStatus.REJECTED, reviewerId, notes);
    }

    public boolean isPending() {
        return status == ApprovalStatus.PENDING;
    }

    private void decide(ApprovalStatus decision, String reviewerId, String notes) {
        DomainGuard.notBlank(reviewerId, "reviewerId");
        if (status != ApprovalStatus.PENDING) {
            throw new ApprovalAlreadyDecidedException(getId(), status);
        }
        this.status = decision;
        this.reviewedById = reviewerId;
        this.reviewNotes = notes;
        this.reviewedAt = LocalDateTime.now();
    }
}
