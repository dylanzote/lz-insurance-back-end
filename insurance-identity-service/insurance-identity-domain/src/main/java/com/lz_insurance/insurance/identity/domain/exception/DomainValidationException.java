package com.lz_insurance.insurance.identity.domain.exception;

import com.lz_insurance.core.exception.ValidationException;
import lombok.Getter;

/**
 * Raised when a domain model is constructed or mutated into an invalid state
 * (null required fields, broken invariants). Domain rules are expressed here,
 * never via javax/jakarta bean-validation annotations on the model.
 *
 * <p>Unlike the core {@link ValidationException}, this keeps the offending field
 * AND surfaces the rule text in {@link #getMessage()} so failures are legible in
 * logs and assertions, not buried in a field-error map.
 */
@Getter
public class DomainValidationException extends ValidationException {

    private final String field;

    public DomainValidationException(String message) {
        super(message);
        this.field = null;
    }

    public DomainValidationException(String field, String error) {
        super("%s: %s".formatted(field, error));
        this.field = field;
    }
}
