package com.lz_insurance.insurance.identity.domain.dto;

import com.lz_insurance.insurance.identity.domain.port.out.KeycloakUserPort;

/**
 * Domain-owned input for {@link KeycloakUserPort#updateUser}. Mirrors {@link KeycloakUserRegistration}
 * in keeping the security-module SPI types out of the domain port. All fields are optional; a
 * {@code null} field means "leave unchanged". Disabling an account is expressed as
 * {@code enabled = Boolean.FALSE}.
 */
public record KeycloakUserUpdate(
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        Boolean enabled) {

    /** Convenience for the common deactivate path. */
    public static KeycloakUserUpdate disable() {
        return new KeycloakUserUpdate(null, null, null, null, Boolean.FALSE);
    }
}
