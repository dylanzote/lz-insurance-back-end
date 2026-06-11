package com.lz_Insurance.core.util;

import lombok.experimental.UtilityClass;

import java.util.UUID;

@UtilityClass
public class IdGenerator {

    public String generate() {
        return UUID.randomUUID().toString();
    }

    /**
     * Generates a UUID from a string (useful for testing or when you need a specific ID).
     */
    public String fromString(String uuidString) {
        return UUID.fromString(uuidString).toString();
    }

    /**
     * Generates a UUID based on the given name (deterministic).
     */
    public String fromName(String name) {
        return UUID.nameUUIDFromBytes(name.getBytes()).toString();
    }

    /**
     * Validates if a string is a valid UUID.
     */
    public boolean isValid(String id) {
        if (id == null || id.isEmpty()) {
            return false;
        }
        try {
            UUID.fromString(id);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
