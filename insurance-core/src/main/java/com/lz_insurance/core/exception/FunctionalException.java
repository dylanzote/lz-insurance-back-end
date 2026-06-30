package com.lz_insurance.core.exception;

import lombok.Getter;

@Getter
public class FunctionalException extends InsuranceException {

    public FunctionalException(ErrorCode errorCode) {
        super(errorCode);
    }

    public FunctionalException(String message) {
        super(ErrorCode.BAD_REQUEST, message);
    }

    public FunctionalException(String message, Throwable cause) {
        super(ErrorCode.BAD_REQUEST, message, cause);
    }
}
