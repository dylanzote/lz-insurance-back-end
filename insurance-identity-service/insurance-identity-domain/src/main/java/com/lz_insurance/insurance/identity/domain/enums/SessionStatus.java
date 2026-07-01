package com.lz_insurance.insurance.identity.domain.enums;

/**
 * State of a tracked session. ACTIVE is the only non-terminal state.
 */
public enum SessionStatus {
    ACTIVE,
    EXPIRED,
    REVOKED
}
