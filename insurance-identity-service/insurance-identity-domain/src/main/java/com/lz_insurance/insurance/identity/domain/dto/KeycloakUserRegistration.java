package com.lz_insurance.insurance.identity.domain.dto;

import com.lz_insurance.insurance.identity.domain.port.out.KeycloakUserPort;

/**
 * Domain-owned input for {@link KeycloakUserPort#createUser}. Kept provider-agnostic so the
 * security module's SPI types never leak into the domain — the infrastructure adapter maps this
 * to whatever the identity provider requires.
 *
 * <p>Note there is no "required actions" field: first-login password change is enforced by the
 * domain via {@code IdentityProfile.passwordChangeRequired}, never delegated to the provider.
 */
public record KeycloakUserRegistration(
        String username,
        String email,
        String firstName,
        String lastName,
        String temporaryPassword,
        boolean emailVerified,
        boolean enabled) {
}
