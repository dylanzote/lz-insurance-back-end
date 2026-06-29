package com.lz_Insurance.insurance.identity.domain.exception;

import com.lz_Insurance.core.exception.FunctionalException;
import com.lz_Insurance.insurance.identity.domain.enumeration.ApprovalStatus;

/**
 * Raised when {@code approve()} or {@code reject()} is invoked on an
 * {@code ApprovalRequest} that has already left the PENDING state.
 */
public class ApprovalAlreadyDecidedException extends FunctionalException {

    public ApprovalAlreadyDecidedException(String requestId, ApprovalStatus currentStatus) {
        super("Approval request %s is already %s and cannot be decided again".formatted(requestId, currentStatus));
    }
}
