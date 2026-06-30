package com.lz_insurance.messaging.exception;

import com.lz_insurance.core.exception.ErrorCode;
import com.lz_insurance.core.exception.InsuranceException;

public class KafkaRetryableException extends InsuranceException {

    public KafkaRetryableException(ErrorCode errorCode) {
        super(errorCode);
    }

    public KafkaRetryableException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public KafkaRetryableException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
