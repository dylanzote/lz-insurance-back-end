package com.lz_insurance.insurance.identity.domain.enums;

/**
 * Data visibility scope applied to a permission grant.
 * Enforced at the JPA Specification layer via predicate injection.
 */
public enum OrganizationScope {
    OWN,
    BRANCH,
    ALL
}
