package com.lz_insurance.storage.exception;

import com.lz_insurance.core.exception.ErrorCode;
import com.lz_insurance.core.exception.InsuranceException;

public class MinioException extends InsuranceException {

    public MinioException(ErrorCode errorCode) {
        super(errorCode);
    }

    public MinioException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public MinioException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
