package com.lz_insurance.security.keycloak.service;

import com.lz_insurance.core.exception.FunctionalException;
import com.lz_insurance.security.keycloak.model.KeyCloakUser;
import com.lz_insurance.security.keycloak.model.KeycloakProperties;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.UserSessionRepresentation;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
public class KeyCloakService {

    private final Keycloak keycloak;

    private final KeycloakProperties keycloakProperties;

    private static final String CLIENT_CREDENTIALS_GRANT_TYPE = "client_credentials";
    private static final String PASSWORD_GRANT_TYPE = "password";
    private static final String REFRESH_TOKEN_GRANT_TYPE = "refresh_token";
    private static final String CLIENT_ID = "client_id";
    private static final String CLIENT_SECRET = "client_secret";
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";
    private static final String GRANT_TYPE = "grant_type";


    public List<KeyCloakUser> getUsers() {
        log.info("Fetching all Keycloak users");
        return keycloak
                .realm(keycloakProperties.getRealm())
                .users()
                .list()
                .stream()
                .map(KeyCloakUser::toUser)
                .collect(Collectors.toList());
    }

    public KeyCloakUser getUserById(String userId) {
        log.info("getting keycloak user with id {}", userId);
        UserResource userResource = getUserResource(userId);
        var user =  KeyCloakUser.toUser(userResource.toRepresentation());
        user.setRoles(getUserRoles(userId));
        return user;
    }

    public KeyCloakUser getUserByUsername(String username) {
        log.info("Fetching Keycloak user by username: {}", username);
        List<UserRepresentation> results = keycloak
                .realm(keycloakProperties.getRealm())
                .users()
                .searchByUsername(username, true);

        if (results.isEmpty()) {
            throw new FunctionalException("User not found with username: " + username);
        }

        UserRepresentation rep = results.getFirst();
        var user = KeyCloakUser.toUser(rep);
        user.setRoles(getUserRoles(rep.getId()));
        return user;
    }

    public KeyCloakUser getUserByEmail(String email) {
        log.info("Fetching Keycloak user by email: {}", email);
        List<UserRepresentation> results = keycloak
                .realm(keycloakProperties.getRealm())
                .users()
                .searchByEmail(email, true);

        if (results.isEmpty()) {
            throw new FunctionalException("User not found with email: " + email);
        }

        UserRepresentation rep = results.getFirst();
        var user = KeyCloakUser.toUser(rep);
        user.setRoles(getUserRoles(rep.getId()));
        return user;
    }

    public String createUser(KeyCloakUser keyCloakUser) {
        log.info("Creating Keycloak user with email: {}", keyCloakUser.getEmail());
        var response = keycloak
               .realm(keycloakProperties.getRealm())
               .users()
               .create(keyCloakUser.toUserRepresentation());
        if (!Objects.equals(201, response.getStatus())) {
            log.error("Failed to create user in Keycloak. Status: {}", response.getStatus());
            throw new FunctionalException("User could not be created in Keycloak. Status: " + response.getStatus());
        }

        // Get the created user ID from the location header
        String location = response.getLocation().toString();
        String userId = location.substring(location.lastIndexOf('/') + 1);

        if (keyCloakUser.getRoles() != null && !keyCloakUser.getRoles().isEmpty()) {
            keyCloakUser.getRoles()
                    .forEach(role -> assignRoleToUser(userId, role));
        }

        log.info("User created successfully with ID: {}", userId);
        return userId;
    }

    public void updateUser(KeyCloakUser keyCloakUser) {
        log.info("Updating keycloak user with id {}", keyCloakUser.getId());
        getUserResource(keyCloakUser.getId())
                .update(keyCloakUser.toUpdateUserRepresentation());
        if (keyCloakUser.getRoles() != null) {
            List<String> current = getUserRoles(keyCloakUser.getId());

            current.stream()
                    .filter(r -> !keyCloakUser.getRoles().contains(r))
                    .forEach(r -> removeRoleFromUser(keyCloakUser.getId(), r));

            keyCloakUser.getRoles().stream()
                    .filter(r -> !current.contains(r))
                    .forEach(r -> assignRoleToUser(keyCloakUser.getId(), r));
        }

        log.info("Keycloak user {} updated", keyCloakUser.getId());
    }

     public void deleteUser(String userId) {
        log.info("Deleting keycloak user with id {}", userId);
        keycloak
               .realm(keycloakProperties.getRealm())
               .users()
               .delete(userId);
    }

    public void sendEmailVerification(String userId) {
        log.info("Sending email verification for user with id {}", userId);
        getUserResource(userId)
               .sendVerifyEmail();
    }

    public void resetPassword(String userId, String password, boolean temporary) {
        log.info("Resetting password for user with id {}", userId);
        CredentialRepresentation credentialRepresentation = new CredentialRepresentation();
        credentialRepresentation.setTemporary(temporary);
        credentialRepresentation.setValue(password);
        credentialRepresentation.setType(CredentialRepresentation.PASSWORD);

        getUserResource(userId)
               .resetPassword(credentialRepresentation);
    }

    public List<String> getUserRoles(String userId) {
        try {
            return keycloak
                    .realm(keycloakProperties.getRealm())
                    .users()
                    .get(userId)
                    .roles()
                    .realmLevel()
                    .listAll()
                    .stream()
                    .map(RoleRepresentation::getName)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to fetch roles for user: {}", userId, e);
            return Collections.emptyList();
        }
    }

    public void assignRoleToUser(String userId, String roleName) {
        log.info("Assigning role {} to user with id {}", roleName, userId);
        var userResource = getUserResource(userId);
        var roleRepresentation = getRolesResource().get(roleName).toRepresentation();
        userResource.roles().realmLevel().add(Collections.singletonList(roleRepresentation));
    }

    public void removeRoleFromUser(String userId, String roleName) {
        log.info("Removing role '{}' from user: {}", roleName, userId);
        RoleRepresentation role = getRolesResource().get(roleName).toRepresentation();
        if (role != null) {
            getUserResource(userId).roles().realmLevel().remove(Collections.singletonList(role));
        } else {
            log.warn("Role '{}' not found — nothing to remove from user: {}", roleName, userId);
        }
    }

    public List<String> getRoles() {
        log.info("Getting all roles");
        return keycloak
                .realm(keycloakProperties.getRealm())
                .roles()
                .list()
                .stream()
                .map(RoleRepresentation::getName)
                .collect(Collectors.toList());
    }

    public void addRealmRole(String role, String description) {
        log.info("Adding realm role {}", role);
        if(!getRoles().contains(role)) {
            RoleRepresentation roleRep = new  RoleRepresentation();
            roleRep.setName(role);
            roleRep.setDescription(description);
            getRolesResource().create(roleRep);
        } else {
            log.warn("Realm role '{}' already exists — skipping creation", role);
        }
    }

    public void deleteRealmRole(String role) {
        log.info("Deleting realm role {}", role);
        getRolesResource().deleteRole(role);
    }


    public List<UserSessionRepresentation> getUserSessions(String userId) {
        return keycloak
                .realm(keycloakProperties.getRealm())
                .users()
                .get(userId)
                .getUserSessions();
    }

    public void logoutUser(String userId) {
        log.info("Logging out user: {}", userId);
        keycloak
                .realm(keycloakProperties.getRealm())
                .users()
                .get(userId)
                .logout();
        log.info("User {} logged out successfully", userId);
    }

    public void revokeSession(String sessionId) {
        keycloak
                .realm(keycloakProperties.getRealm())
                .deleteSession(sessionId, false);
        log.info("Session {} has been revoked.", sessionId);
    }

    public void revokeAllOtherSessions(String userId, String currentSessionId) {
        log.info("Revoking all other sessions for user: {}", userId);
        List<UserSessionRepresentation> sessions = getUserSessions(userId);
        int revokedCount = 0;

        for (UserSessionRepresentation session : sessions) {
            if (session.getId() != null && !session.getId().equals(currentSessionId)) {
                try {
                    revokeSession(session.getId());
                    revokedCount++;
                } catch (Exception e) {
                    log.warn("Failed to revoke session {}: {}", session.getId(), e.getMessage());
                }
            }
        }
        log.info("Revoked {} sessions for user {}, kept session: {}", revokedCount, userId, currentSessionId);
    }

    public int getActiveSessionCount(String userId) {
        return getUserSessions(userId).size();
    }

    public MultiValueMap<String, String> buildClientCredentialsRequest() {
        MultiValueMap<String, String> data = new LinkedMultiValueMap<>();
        data.add(CLIENT_ID,     keycloakProperties.getClientId());
        data.add(CLIENT_SECRET, keycloakProperties.getClientSecret());
        data.add(GRANT_TYPE,    CLIENT_CREDENTIALS_GRANT_TYPE);
        return data;
    }

    public MultiValueMap<String, String> buildPasswordGrantRequest(String username, String password) {
        MultiValueMap<String, String> data = new LinkedMultiValueMap<>();
        data.add(CLIENT_ID,     keycloakProperties.getClientId());
        data.add(CLIENT_SECRET, keycloakProperties.getClientSecret());
        data.add(GRANT_TYPE,    PASSWORD_GRANT_TYPE);
        data.add(USERNAME,      username);
        data.add(PASSWORD,      password);
        return data;
    }

    public MultiValueMap<String, String> buildRefreshTokenRequest(String refreshToken) {
        MultiValueMap<String, String> data = new LinkedMultiValueMap<>();
        data.add(CLIENT_ID,     keycloakProperties.getClientId());
        data.add(CLIENT_SECRET, keycloakProperties.getClientSecret());
        data.add(GRANT_TYPE,    REFRESH_TOKEN_GRANT_TYPE);
        data.add(REFRESH_TOKEN_GRANT_TYPE, refreshToken);
        return data;
    }

    public UserRepresentation getUserRepresentation(String userId) {
        return getUserResource(userId).toRepresentation();
    }

    private UserResource getUserResource(String userId) {
        return keycloak
                .realm(keycloakProperties.getRealm())
                .users()
                .get(userId);
    }

    private RolesResource getRolesResource() {
        return keycloak
               .realm(keycloakProperties.getRealm())
               .roles();
    }

}
