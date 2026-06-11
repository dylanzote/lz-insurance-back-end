package com.lz_Insurance.security.core.service;

import com.lz_Insurance.security.core.model.AuthUser;
import com.lz_Insurance.security.core.web.JwtConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final JwtConverter jwtConverter;

    public AuthUser getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (isUnauthenticated(authentication)) return AuthUser.anonymous();

        if (authentication instanceof JwtAuthenticationToken jwtAuth && jwtAuth.getPrincipal() instanceof AuthUser user) {
            return user;
        }
        return AuthUser.anonymous();
    }

    public String getCurrentUserId() {
        return getCurrentUser().getUserId();
    }

    public String getCurrentTenantId() {
        return getCurrentUser().getTenantId();
    }

    public boolean hasRole(String role) {
        AuthUser user = getCurrentUser();
        return user.getRoles() != null && user.getRoles().contains(role);
    }

    public boolean hasPermission(String permission) {
        return getCurrentUser().hasPermission(permission);
    }

    private boolean isUnauthenticated(Authentication auth) {
        return auth == null
                || !auth.isAuthenticated()
                || auth instanceof AnonymousAuthenticationToken;
    }
}
