package com.lz_insurance.insurance.identity.domain.enums;

/**
 * State of an {@code ApprovalRequest}. PENDING is the only non-terminal state.
 */
public enum ApprovalStatus {
    PENDING,
    APPROVED,
    REJECTED
}
