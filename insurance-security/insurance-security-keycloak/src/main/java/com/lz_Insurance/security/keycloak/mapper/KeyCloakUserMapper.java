package com.lz_Insurance.security.keycloak.mapper;

import com.lz_Insurance.security.core.model.AuthUser;
import com.lz_Insurance.security.core.spi.CreateUserRequest;
import com.lz_Insurance.security.core.spi.UserSession;
import com.lz_Insurance.security.keycloak.model.KeyCloakUser;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.UserSessionRepresentation;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class KeyCloakUserMapper {

    public KeyCloakUser fromCreateRequest(CreateUserRequest request) {
        return KeyCloakUser.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .username(request.getUsername() != null ? request.getUsername() : request.getEmail())
                .password(request.getPassword())
                .enabled(request.isEnabled())
                .emailVerified(request.isEmailVerified())
                .roles(request.getRoles())
                .attributes(convertToKeycloakAttributes(request.getAttributes()))
                .build();
    }

    public AuthUser toAuthUser(UserRepresentation user, List<String> roles) {
        if (user == null) {
            return null;
        }

        return AuthUser.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .roles(roles != null ? roles : Collections.emptyList())
                .emailVerified(user.isEmailVerified())
                .enabled(user.isEnabled())
                .attributes(convertAttributes(user.getAttributes()))
                .build();
    }

    public UserSession toUserSession(UserSessionRepresentation session) {
        if (session == null) {
            return null;
        }

        return UserSession.builder()
                .id(session.getId())
                .userId(session.getUserId())
                .username(session.getUsername())
                .ipAddress(session.getIpAddress())
                .startedAt(LocalDateTime.ofEpochSecond(session.getStart(), 0, ZoneOffset.UTC))
                .lastAccessedAt(LocalDateTime.ofEpochSecond(session.getLastAccess(), 0, ZoneOffset.UTC))
                .clients(session.getClients())
                .build();
    }

    public List<UserSession> toUserSessions(List<UserSessionRepresentation> sessions) {
        if (sessions == null) {
            return Collections.emptyList();
        }
        return sessions.stream()
                .map(this::toUserSession)
                .collect(Collectors.toList());
    }

    public Map<String, Object> convertAttributes(Map<String, List<String>> attributes) {
        if (attributes == null) {
            return Collections.emptyMap();
        }

        Map<String, Object> result = new java.util.HashMap<>();
        attributes.forEach((key, value) -> {
            if (value != null && !value.isEmpty()) {
                result.put(key, value.size() == 1 ? value.get(0) : value);
            }
        });
        return result;
    }

    public Map<String, List<String>> convertToKeycloakAttributes(Map<String, Object> attributes) {
        if (attributes == null) {
            return Collections.emptyMap();
        }

        Map<String, List<String>> result = new java.util.HashMap<>();
        attributes.forEach((key, value) -> {
            if (value != null) {
                if (value instanceof List) {
                    result.put(key, ((List<?>) value).stream()
                            .map(Object::toString)
                            .collect(Collectors.toList()));
                } else {
                    result.put(key, List.of(value.toString()));
                }
            }
        });
        return result;
    }
}
