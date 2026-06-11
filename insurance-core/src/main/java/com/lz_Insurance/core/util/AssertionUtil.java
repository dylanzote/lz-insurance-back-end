package com.lz_Insurance.core.util;



import com.lz_Insurance.core.exception.ValidationException;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Map;

/**
 * Utility class for defensive programming and assertions.
 */
public final class AssertionUtil {

    private AssertionUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static void notNull(Object object, String fieldName) {
        if (object == null) {
            throw new ValidationException(fieldName, fieldName + " cannot be null");
        }
    }

    public static void notBlank(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new ValidationException(fieldName, fieldName + " cannot be blank");
        }
    }

    public static void notEmpty(Collection<?> collection, String fieldName) {
        if (collection == null || collection.isEmpty()) {
            throw new ValidationException(fieldName, fieldName + " cannot be empty");
        }
    }

    public static void notEmpty(Map<?, ?> map, String fieldName) {
        if (map == null || map.isEmpty()) {
            throw new ValidationException(fieldName, fieldName + " cannot be empty");
        }
    }

    public static void isTrue(boolean condition, String fieldName, String message) {
        if (!condition) {
            throw new ValidationException(fieldName, message);
        }
    }

    public static void isFalse(boolean condition, String fieldName, String message) {
        if (condition) {
            throw new ValidationException(fieldName, message);
        }
    }

    public static void isPositive(Number number, String fieldName) {
        if (number == null || number.doubleValue() <= 0) {
            throw new ValidationException(fieldName, fieldName + " must be positive");
        }
    }

    public static void isNonNegative(Number number, String fieldName) {
        if (number == null || number.doubleValue() < 0) {
            throw new ValidationException(fieldName, fieldName + " cannot be negative");
        }
    }

    public static void isValidEmail(String email) {
        if (!StringUtils.hasText(email)) {
            throw new ValidationException("email", "Email cannot be blank");
        }

        String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
        if (!email.matches(emailRegex)) {
            throw new ValidationException("email", "Invalid email format");
        }
    }
}
