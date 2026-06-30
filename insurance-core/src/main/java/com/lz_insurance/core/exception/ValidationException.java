package com.lz_insurance.core.exception;

import lombok.Getter;

import java.util.Map;

/**
 * Exception for validation failures with field-specific errors.
 */
@Getter
public class ValidationException extends InsuranceException {

    private final Map<String, String> fieldErrors;

    public ValidationException(Map<String, String> fieldErrors) {
        super(ErrorCode.VALIDATION_FAILED, "Validation failed for one or more fields");
        this.fieldErrors = fieldErrors;
    }

    public ValidationException(String message) {
        super(ErrorCode.VALIDATION_FAILED, message);
        this.fieldErrors = Map.of();
    }

    public ValidationException(String field, String error) {
        super(ErrorCode.VALIDATION_FAILED, "Validation failed");
        this.fieldErrors = Map.of(field, error);
    }
}
