package com.lz_insurance.insurance.identity.domain.enumeration;

/**
 * State of an {@code ApprovalRequest}. PENDING is the only non-terminal state.
 */
public enum ApprovalStatus {
    PENDING,
    APPROVED,
    REJECTED
}
