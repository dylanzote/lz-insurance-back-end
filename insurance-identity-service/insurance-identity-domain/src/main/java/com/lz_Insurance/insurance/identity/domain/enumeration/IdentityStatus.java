package com.lz_Insurance.insurance.identity.domain.enumeration;

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
