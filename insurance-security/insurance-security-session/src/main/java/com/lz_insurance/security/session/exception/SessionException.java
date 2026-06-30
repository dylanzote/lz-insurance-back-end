package com.lz_insurance.security.session.exception;

import com.lz_insurance.core.exception.ErrorCode;
import com.lz_insurance.core.exception.InsuranceException;

public class SessionException extends InsuranceException {

    public SessionException(ErrorCode errorCode) {
        super(errorCode);
    }

    public SessionException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public SessionException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
