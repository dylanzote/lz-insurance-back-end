package com.lz_Insurance.messaging.exception;

import com.lz_Insurance.core.exception.ErrorCode;
import com.lz_Insurance.core.exception.InsuranceException;

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
