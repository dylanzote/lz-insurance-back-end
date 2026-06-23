package com.lz_Insurance.insurance.identity.domain.enumeration;

/**
 * State of an {@code ApprovalRequest}. PENDING is the only non-terminal state.
 */
public enum ApprovalStatus {
    PENDING,
    APPROVED,
    REJECTED
}
