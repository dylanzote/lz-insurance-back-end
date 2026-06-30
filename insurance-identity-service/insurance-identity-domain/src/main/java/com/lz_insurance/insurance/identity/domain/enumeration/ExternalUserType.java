package com.lz_insurance.insurance.identity.domain.enumeration;

/**
 * Sub-type of an EXTERNAL actor (customer). Null for INTERNAL actors.
 */
public enum ExternalUserType {
    POLICYHOLDER,
    CLAIMANT,
    BENEFICIARY
}
