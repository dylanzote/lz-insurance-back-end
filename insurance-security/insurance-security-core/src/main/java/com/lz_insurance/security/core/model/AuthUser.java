package com.lz_insurance.security.core.model;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class AuthUser implements Serializable {
    private String userId;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String tenantId;
    private List<String> tenants;
    private List<String> roles;
    private List<String> permissions;
    private String sessionId;
    private String deviceId;
    private String deviceType;
    private String ipAddress;
    private String userAgent;
    private Map<String, Object> attributes;
    private boolean emailVerified;
    private boolean enabled;

    public static AuthUser anonymous() {
        return AuthUser.builder()
            .userId("anonymous")
            .username("anonymous")
            .email("anonymous@system.com")
            .roles(List.of("ANONYMOUS"))
            .permissions(List.of())
            .attributes(Map.of())
            .emailVerified(false)
            .enabled(true)
            .build();
    }

    public boolean isAuthenticated() {
        return !"anonymous".equals(userId);
    }

    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    public boolean hasPermission(String permission) {
        if (permissions == null) return false;
        if (permissions.contains(permission)) return true;
        String wildcard = permission.contains(":")
                ? permission.substring(0, permission.indexOf(':')) + ":*"
                : null;
        return wildcard != null && permissions.contains(wildcard);
    }
}
