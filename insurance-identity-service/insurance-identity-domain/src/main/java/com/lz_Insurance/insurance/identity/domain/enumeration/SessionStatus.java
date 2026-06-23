package com.lz_Insurance.insurance.identity.domain.enumeration;

/**
 * State of a tracked session. ACTIVE is the only non-terminal state.
 */
public enum SessionStatus {
    ACTIVE,
    EXPIRED,
    REVOKED
}
