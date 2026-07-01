package com.lz_insurance.insurance.identity.infrastructure.keycloak;

import com.lz_insurance.insurance.identity.domain.exception.IdentityProviderException;
import com.lz_insurance.insurance.identity.domain.port.out.KeycloakUserPort;
import com.lz_insurance.insurance.identity.domain.dto.KeycloakUserRegistration;
import com.lz_insurance.insurance.identity.domain.dto.KeycloakUserUpdate;
import com.lz_insurance.security.core.model.AuthUser;
import com.lz_insurance.security.core.spi.CreateUserRequest;
import com.lz_insurance.security.core.spi.IdentityProvider;
import com.lz_insurance.security.core.spi.UpdateUserRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * Infrastructure adapter for {@link KeycloakUserPort}. It is intentionally thin: it maps the
 * domain-owned request records onto the security module's {@link IdentityProvider} SPI and
 * delegates. Crossing the SPI (rather than the Keycloak client directly) keeps this adapter
 * decoupled from the provider implementation — swapping the provider leaves it untouched.
 *
 * <p>Its one real responsibility beyond mapping is the boundary contract: every provider failure is
 * translated into {@link IdentityProviderException} so no provider SDK exception escapes the port.
 *
 * <p>Gated on {@code insurance.security.idp.type=keycloak} so it (and the underlying provider chain)
 * load only when the identity provider is actually configured.
 */
@Component
@ConditionalOnProperty(prefix = "insurance.security.idp", name = "type", havingValue = "keycloak")
public class KeycloakUserAdapter implements KeycloakUserPort {

    private final IdentityProvider identityProvider;

    public KeycloakUserAdapter(IdentityProvider identityProvider) {
        this.identityProvider = identityProvider;
    }

    @Override
    public String createUser(KeycloakUserRegistration registration) {
        CreateUserRequest request = CreateUserRequest.builder()
                .username(registration.username())
                .email(registration.email())
                .firstName(registration.firstName())
                .lastName(registration.lastName())
                .password(registration.temporaryPassword())
                .emailVerified(registration.emailVerified())
                .enabled(registration.enabled())
                .build();

        AuthUser created = call(() -> identityProvider.createUser(request), "create identity-provider account for " + registration.email());
        return created.getUserId();
    }

    @Override
    public void assignRealmRole(String keycloakUserId, String roleName) {
        run(() -> identityProvider.assignRole(keycloakUserId, roleName), "assign role %s to user %s".formatted(roleName, keycloakUserId));
    }

    @Override
    public void updateUser(String keycloakUserId, KeycloakUserUpdate update) {
        UpdateUserRequest request = UpdateUserRequest.builder()
                .firstName(update.firstName())
                .lastName(update.lastName())
                .email(update.email())
                .phoneNumber(update.phoneNumber())
                .enabled(update.enabled())
                .build();

        call(() -> identityProvider.updateUser(keycloakUserId, request), "update user " + keycloakUserId);
    }

    @Override
    public void changePassword(String keycloakUserId, String newPassword, boolean temporary) {
        run(() -> identityProvider.changePassword(keycloakUserId, newPassword, temporary), "change password for user " + keycloakUserId);
    }

    @Override
    public void deleteUser(String keycloakUserId) {
        run(() -> identityProvider.deleteUser(keycloakUserId), "delete user " + keycloakUserId);
    }

    private <T> T call(Supplier<T> operation, String description) {
        try {
            return operation.get();
        } catch (RuntimeException ex) {
            throw new IdentityProviderException("Identity provider failed to " + description, ex);
        }
    }

    private void run(Runnable operation, String description) {
        call(() -> {
            operation.run();
            return null;
        }, description);
    }
}
