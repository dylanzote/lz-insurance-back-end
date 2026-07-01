package com.lz_insurance.insurance.identity.domain.support;

import com.lz_insurance.insurance.identity.domain.exception.DomainValidationException;
import lombok.experimental.UtilityClass;

import java.security.SecureRandom;


/**
 * Password complexity policy — a DOMAIN rule enforced by OUR system BEFORE any Keycloak call
 * (see {@code docs/user-journeys.md} "Keycloak's Exact Role"). Keycloak only ever stores an
 * already-validated credential; it is never the source of a policy rejection.
 *
 * <p>Rules (from the user-journeys password policy): minimum {@value #MIN_LENGTH} characters, with
 * at least one uppercase letter, one lowercase letter, one digit and one special character.
 *
 * <p>Scope: complexity only. Reuse prevention (last-5 history) and expiry/lockout are separate
 * rules that need the {@code PasswordHistory} / {@code LoginAttempt} models — deferred to M6
 * (see known-gaps G-016). This policy is reused by every flow that sets a password (bootstrap,
 * approval-generated credentials, forced change, subsequent change).
 */
@UtilityClass
public class PasswordPolicy {

    public static final int MIN_LENGTH = 12;
    private static final String UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";

    private static final String DIGITS = "0123456789";

    private static final String SPECIAL_CHARS = "!@#$%^&*()_-+=<>?";

    private static final String ALL_CHARACTERS = UPPERCASE + LOWERCASE + DIGITS + SPECIAL_CHARS;

    /** Validates against the policy, attributing failures to a generic {@code "password"} field. */
    public static void validate(String password) {
        validate(password, "password");
    }

    /**
     * Validates {@code password} against the policy, attributing any failure to {@code field}
     * (e.g. {@code "temporaryPassword"} / {@code "newPassword"}) so the error message is legible.
     *
     * @throws DomainValidationException if the password violates the policy (maps to HTTP 400)
     */
    public static void validate(String password, String field) {
        if (password == null || password.isBlank()) {
            throw new DomainValidationException(field, "password must not be blank");
        }
        if (password.length() < MIN_LENGTH) {
            throw new DomainValidationException(field,
                    "password must be at least " + MIN_LENGTH + " characters");
        }

        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;
        for (int i = 0; i < password.length(); i++) {
            char c = password.charAt(i);
            if (Character.isUpperCase(c)) {
                hasUpper = true;
            } else if (Character.isLowerCase(c)) {
                hasLower = true;
            } else if (Character.isDigit(c)) {
                hasDigit = true;
            } else {
                hasSpecial = true;
            }
        }

        if (!hasUpper) {
            throw new DomainValidationException(field, "password must contain at least one uppercase letter");
        }
        if (!hasLower) {
            throw new DomainValidationException(field, "password must contain at least one lowercase letter");
        }
        if (!hasDigit) {
            throw new DomainValidationException(field, "password must contain at least one digit");
        }
        if (!hasSpecial) {
            throw new DomainValidationException(field, "password must contain at least one special character");
        }
    }

    public String generateRandomPassword() {
        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder(MIN_LENGTH);

        for (int i = 0; i < MIN_LENGTH; i++) {
            int index = random.nextInt(ALL_CHARACTERS.length());
            password.append(ALL_CHARACTERS.charAt(index));
        }

        return password.toString();
    }
}
