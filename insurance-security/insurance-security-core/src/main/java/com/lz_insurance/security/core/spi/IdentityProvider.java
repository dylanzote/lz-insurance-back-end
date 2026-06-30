package com.lz_insurance.security.core.spi;

import com.lz_insurance.security.core.model.AuthUser;

import java.util.List;
import java.util.Optional;

public interface IdentityProvider {

    AuthResponse authenticate(String username, String password, String clientId);

    AuthResponse refreshToken(String refreshToken);

    Optional<AuthUser> getUserById(String userId);

    Optional<AuthUser> getUserByUsername(String username);

    Optional<AuthUser> getUserByEmail(String email);

    AuthUser createUser(CreateUserRequest request);

    AuthUser updateUser(String userId, UpdateUserRequest request);

    void deleteUser(String userId);

    void changePassword(String userId, String newPassword, boolean temporary);

    void assignRole(String userId, String role);

    void removeRole(String userId, String role);

    List<String> getUserRoles(String userId);

    void sendEmailVerification(String userId);

    void logout(String userId, String sessionId);

    List<UserSession> getUserSessions(String userId);

    void revokeSession(String userId, String sessionId);

    void revokeAllOtherSessions(String userId, String currentSessionId);

    String getProviderType();
}
