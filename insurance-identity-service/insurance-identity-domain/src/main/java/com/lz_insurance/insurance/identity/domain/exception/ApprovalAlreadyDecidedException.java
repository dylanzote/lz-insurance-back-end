package com.lz_insurance.insurance.identity.domain.exception;

import com.lz_insurance.core.exception.FunctionalException;
import com.lz_insurance.insurance.identity.domain.enums.ApprovalStatus;

/**
 * Raised when {@code approve()} or {@code reject()} is invoked on an
 * {@code ApprovalRequest} that has already left the PENDING state.
 */
public class ApprovalAlreadyDecidedException extends FunctionalException {

    public ApprovalAlreadyDecidedException(String requestId, ApprovalStatus currentStatus) {
        super("Approval request %s is already %s and cannot be decided again".formatted(requestId, currentStatus));
    }
}
