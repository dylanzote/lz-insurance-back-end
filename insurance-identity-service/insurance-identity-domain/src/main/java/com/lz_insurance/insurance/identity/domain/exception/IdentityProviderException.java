package com.lz_insurance.insurance.identity.domain.exception;

import com.lz_insurance.core.exception.FunctionalException;

/**
 * Raised when an identity-provider operation (account create/update/role/delete) fails. The name is
 * deliberately provider-neutral: the domain knows it talks to "an identity provider", not Keycloak
 * specifically. Infrastructure adapters translate raw provider failures into this type so no
 * provider SDK exception ever surfaces past the {@code KeycloakUserPort} boundary.
 */
public class IdentityProviderException extends FunctionalException {

    public IdentityProviderException(String message) {
        super(message);
    }

    public IdentityProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
