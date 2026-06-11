package com.lz_Insurance.product.engine.exception;

import com.lz_Insurance.core.exception.ErrorCode;
import com.lz_Insurance.core.exception.InsuranceException;

public class SchemaValidationException extends InsuranceException {

    public SchemaValidationException(ErrorCode errorCode) {
        super(errorCode);
    }

    public SchemaValidationException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public SchemaValidationException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
