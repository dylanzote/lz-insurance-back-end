package com.lz_insurance.insurance.identity.domain.enumeration;

/**
 * Lifecycle state of an {@code IdentityProfile}.
 * Transitions: PENDING_APPROVAL -> ACTIVE -> SUSPENDED -> DEACTIVATED.
 */
public enum IdentityStatus {
    PENDING_APPROVAL,
    ACTIVE,
    SUSPENDED,
    DEACTIVATED
}
