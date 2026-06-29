package com.lz_Insurance.insurance.identity.domain.enumeration;

/**
 * Sub-type of an INTERNAL actor (staff). Null for EXTERNAL actors.
 */
public enum InternalUserType {
    STAFF,
    AGENT,
    UNDERWRITER,
    CLAIMS_ADJUSTER,
    BRANCH_MANAGER,
    TENANT_ADMIN
}
