package com.lz_Insurance.insurance.identity.domain.model;

import com.lz_Insurance.core.model.BaseDomainEntity;
import com.lz_Insurance.insurance.identity.domain.enumeration.ApprovalStatus;
import com.lz_Insurance.insurance.identity.domain.exception.ApprovalAlreadyDecidedException;
import com.lz_Insurance.insurance.identity.domain.support.DomainGuard;
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
