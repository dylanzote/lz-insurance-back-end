package com.lz_Insurance.insurance.identity.domain.enumeration;

/**
 * Position of a branch in the organizational hierarchy.
 * A HEADQUARTER branch is the root and cannot have a parent.
 */
public enum BranchType {
    HEADQUARTER,
    REGIONAL,
    LOCAL
}
