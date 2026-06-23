package com.lz_Insurance.insurance.identity.domain.enumeration;

/**
 * Data visibility scope applied to a permission grant.
 * Enforced at the JPA Specification layer via predicate injection.
 */
public enum OrganizationScope {
    OWN,
    BRANCH,
    ALL
}
