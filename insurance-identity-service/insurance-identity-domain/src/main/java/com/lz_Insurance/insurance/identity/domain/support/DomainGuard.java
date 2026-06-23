package com.lz_Insurance.insurance.identity.domain.support;

import com.lz_Insurance.insurance.identity.domain.exception.DomainValidationException;

/**
 * Guard clauses for domain-model invariants. Throws {@link DomainValidationException}
 * so the domain layer keeps its own validation contract instead of leaking the
 * generic core {@code ValidationException} into business rules.
 */
public final class DomainGuard {

    private DomainGuard() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static void notBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new DomainValidationException(field, field + " must not be blank");
        }
    }

    public static void notNull(Object value, String field) {
        if (value == null) {
            throw new DomainValidationException(field, field + " must not be null");
        }
    }

    public static void isNull(Object value, String field, String message) {
        if (value != null) {
            throw new DomainValidationException(field, message);
        }
    }

    public static void isTrue(boolean condition, String field, String message) {
        if (!condition) {
            throw new DomainValidationException(field, message);
        }
    }
}
