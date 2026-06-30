package com.lz_insurance.core.exception;

import lombok.Getter;

@Getter
public class InsuranceException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Object[] args;

    public InsuranceException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.args = new Object[0];
    }

    public InsuranceException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.args = new Object[0];
    }

    public InsuranceException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
        this.args = new Object[0];
    }

    public InsuranceException(ErrorCode errorCode, Object... args) {
        super(String.format(errorCode.getMessage(), args));
        this.errorCode = errorCode;
        this.args = args;
    }

    public InsuranceException(ErrorCode errorCode, Throwable cause, Object... args) {
        super(String.format(errorCode.getMessage(), args), cause);
        this.errorCode = errorCode;
        this.args = args;
    }

    public String getFormattedMessage() {
        return String.format(errorCode.getMessage(), args);
    }
}
