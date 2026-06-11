package com.lz_Insurance.security.keycloak;

import com.lz_Insurance.core.exception.FunctionalException;
import com.lz_Insurance.core.request.HttpService;
import com.lz_Insurance.security.core.model.AuthUser;
import com.lz_Insurance.security.core.spi.*;
import com.lz_Insurance.security.keycloak.mapper.KeyCloakUserMapper;
import com.lz_Insurance.security.keycloak.model.KeycloakProperties;
import com.lz_Insurance.security.keycloak.model.TokenData;
import com.lz_Insurance.security.keycloak.service.KeyCloakService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "insurance.security.idp", name = "type", havingValue = "keycloak")
public class KeycloakIdentityProvider implements IdentityProvider {

    private final KeyCloakService keyCloakService;

    private final HttpService httpService;

    private final KeycloakProperties keycloakProperties;

    private final KeyCloakUserMapper userConverter;

    @Override
    public AuthResponse authenticate(String username, String password, String clientId) {
        log.info("Authenticating user: {}", username);
        try {
            var token = httpService.post(keycloakProperties.getAuthServerUrl(), keyCloakService.buildPasswordGrantRequest(username, password), TokenData.class);
            if (token == null || !token.isSuccess()) {
                String detail = token != null ? token.getErrorDescription() : "null response";
                throw new FunctionalException("Authentication failed: " + detail);
            }
            AuthUser authUser = getUserByUsername(username).orElse(null);

            return AuthResponse.builder()
                    .accessToken(token.getAccessToken())
                    .refreshToken(token.getRefreshToken())
                    .tokenType(token.getTokenType())
                    .expiresIn(token.getExpiresIn())
                    .refreshExpiresIn(token.getRefreshExpiresIn())
                    .sessionState(token.getSessionState())
                    .user(authUser)
                    .build();
        } catch (Exception e) {
            log.warn("Authentication failed for user {}: {}", username, e.getMessage());
            throw new FunctionalException("Authentication failed: " + e.getMessage());
        }
    }

    @Override
    public AuthResponse refreshToken(String refreshToken) {
        log.info("Refreshing token");
        try {
            TokenData tokenData = httpService.post(keycloakProperties.getAuthServerUrl(), keyCloakService.buildRefreshTokenRequest(refreshToken), TokenData.class);
            if (tokenData == null || !tokenData.isSuccess()) {
                log.error("Token refresh failed");
                throw new FunctionalException("Token refresh failed: " +
                    (tokenData != null ? tokenData.getErrorDescription() : "Unknown error"));
            }

            return AuthResponse.builder()
                    .accessToken(tokenData.getAccessToken())
                    .refreshToken(tokenData.getRefreshToken())
                    .tokenType(tokenData.getTokenType())
                    .expiresIn(tokenData.getExpiresIn())
                    .refreshExpiresIn(tokenData.getRefreshExpiresIn())
                    .sessionState(tokenData.getSessionState())
                    .build();

        } catch (Exception e) {
            log.error("Token refresh failed", e);
            throw new FunctionalException("Token refresh failed: " + e.getMessage(), e);
        }
    }


    @Override
    public Optional<AuthUser> getUserById(String userId) {
        log.info("Getting user by id: {}", userId);
        try {
            var keycloakUser  = keyCloakService.getUserById(userId);
            return Optional.of(userConverter.toAuthUser(keyCloakService.getUserRepresentation(userId), keycloakUser.getRoles()));
        } catch (Exception e) {
            log.error("Failed to get user by id: {}", userId, e);
            return Optional.empty();
        }
    }

    @Override
    public Optional<AuthUser> getUserByUsername(String username) {
        log.info("Getting user by username: {}", username);
        try {
            var keycloakUser = keyCloakService.getUserByUsername(username);
            return Optional.of(userConverter.toAuthUser(keyCloakService.getUserRepresentation(keycloakUser.getId()), keycloakUser.getRoles()));
        } catch (Exception e) {
            log.error("Failed to get user by username: {}", username, e);
            return Optional.empty();
        }
    }

    @Override
    public Optional<AuthUser> getUserByEmail(String email) {
        log.info("Getting user by email: {}", email);
        try {
            var keycloakUser = keyCloakService.getUserByEmail(email);
            return Optional.of(userConverter.toAuthUser(keyCloakService.getUserRepresentation(keycloakUser.getId()), keycloakUser.getRoles()));
        } catch (Exception e) {
            log.error("Failed to get user by email: {}", email, e);
            return Optional.empty();
        }
    }

    @Override
    public AuthUser createUser(CreateUserRequest request) {
        log.info("Creating user with email: {}", request.getEmail());
        var keycloakUser = userConverter.fromCreateRequest(request);
        var userId = keyCloakService.createUser(keycloakUser);

        return getUserById(userId).orElseThrow(() ->
            new FunctionalException("User created but not found"));
    }

    @Override
    public AuthUser updateUser(String userId, UpdateUserRequest request) {
        log.info("Updating user: {}", userId);
        var existing = keyCloakService.getUserById(userId);

        if (request.getFirstName()  != null) existing.setFirstName(request.getFirstName());
        if (request.getLastName()   != null) existing.setLastName(request.getLastName());
        if (request.getEmail()      != null) existing.setEmail(request.getEmail());
        if (request.getEnabled()    != null) existing.setEnabled(request.getEnabled());
        if (request.getAttributes() != null) {
            existing.setAttributes(userConverter.convertToKeycloakAttributes(request.getAttributes()));
        }

        keyCloakService.updateUser(existing);

        return getUserById(userId).orElseThrow(() ->
                new FunctionalException("User was updated but could not be retrieved. ID: " + userId));
    }

    @Override
    public void deleteUser(String userId) {
        log.info("Deleting user: {}", userId);
        keyCloakService.deleteUser(userId);
    }

    @Override
    public void changePassword(String userId, String newPassword, boolean temporary) {
        log.info("Changing password for user: {}", userId);
        keyCloakService.resetPassword(userId, newPassword, temporary);
    }

    @Override
    public void assignRole(String userId, String role) {
        log.info("Assigning role {} to user: {}", role, userId);
        keyCloakService.assignRoleToUser(userId, role);
    }

    @Override
    public void removeRole(String userId, String role) {
        log.info("Removing role {} from user: {}", role, userId);
        keyCloakService.removeRoleFromUser(userId, role);
    }

    @Override
    public List<String> getUserRoles(String userId) {
        log.info("Getting roles for user: {}", userId);
        return keyCloakService.getUserRoles(userId);
    }

    @Override
    public void sendEmailVerification(String userId) {
        log.info("Sending email verification to user: {}", userId);
        keyCloakService.sendEmailVerification(userId);
    }

    @Override
    public void logout(String userId, String sessionId) {
        log.info("Logging out user: {} with session: {}", userId, sessionId);
        if (sessionId != null && !sessionId.isEmpty()) {
            keyCloakService.revokeSession(sessionId);
        } else {
            keyCloakService.logoutUser(userId);
        }
    }

    @Override
    public List<UserSession> getUserSessions(String userId) {
        log.info("Getting sessions for user: {}", userId);
        var sessions = keyCloakService.getUserSessions(userId);
        return userConverter.toUserSessions(sessions);
    }

    @Override
    public void revokeSession(String userId, String sessionId) {
        log.info("Revoking session: {} for user: {}", sessionId, userId);
        keyCloakService.revokeSession(sessionId);
    }

    @Override
    public void revokeAllOtherSessions(String userId, String currentSessionId) {
        log.info("Revoking all other sessions for user: {}", userId);
        keyCloakService.revokeAllOtherSessions(userId, currentSessionId);
    }

    @Override
    public String getProviderType() {
        return "keycloak";
    }
}
