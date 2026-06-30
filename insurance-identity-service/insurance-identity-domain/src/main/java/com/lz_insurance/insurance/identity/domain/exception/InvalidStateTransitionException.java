package com.lz_insurance.insurance.identity.domain.exception;

import com.lz_insurance.core.exception.FunctionalException;

/**
 * Raised when a domain state machine is asked to make an illegal transition
 * (e.g. activating an already-deactivated profile, revoking an expired session).
 */
public class InvalidStateTransitionException extends FunctionalException {

    public InvalidStateTransitionException(String message) {
        super(message);
    }

    public InvalidStateTransitionException(Enum<?> from, Enum<?> to, String entity) {
        super("Illegal %s transition: %s -> %s".formatted(entity, from, to));
    }
}
