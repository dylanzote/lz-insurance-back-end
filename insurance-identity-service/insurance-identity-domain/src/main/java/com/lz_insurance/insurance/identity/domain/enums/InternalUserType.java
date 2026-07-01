package com.lz_insurance.insurance.identity.domain.enums;

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
