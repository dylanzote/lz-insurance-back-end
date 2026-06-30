package com.lz_insurance.security.core.web;


import com.lz_insurance.security.core.model.AuthUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Slf4j
@Component
@RequiredArgsConstructor
public class JwtConverter implements Converter<Jwt, AbstractAuthenticationToken> {


    private static final String ROLE_PREFIX = "ROLE_";
    private static final String TENANT_PREFIX = "TENANT_";

    @Value("${security.keycloak.client-id}")
    private String keycloakClientId;

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {

        AuthUser user = extractUser(jwt);

        if (user == null) {
            log.warn("JWT extraction failed — falling back to anonymous principal");
            return anonymousToken(jwt);
        }

        Collection<GrantedAuthority> authorities = buildAuthorities(user);
        // ✅ Store AuthUser as principal so SecurityContext carries it directly
        return new JwtAuthenticationToken(jwt, authorities, user.getUsername()) {
            @Override
            public Object getPrincipal() {
                return user;
            }
        };
    }


    private AuthUser extractUser(Jwt jwt) {
        try {
            String userId   = jwt.getClaimAsString("sub");
            String username = jwt.getClaimAsString("preferred_username");
            String email    = jwt.getClaimAsString("email");

            return AuthUser.builder()
                    .userId(userId)
                    .username(username != null ? username : email)
                    .email(email)
                    .firstName(jwt.getClaimAsString("given_name"))
                    .lastName(jwt.getClaimAsString("family_name"))
                    .tenantId(jwt.getClaimAsString("tenant_id"))
                    .sessionId(jwt.getClaimAsString("session_state"))
                    .emailVerified(Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified")))
                    .enabled(true)
                    .roles(extractRoles(jwt))
                    .permissions(Collections.emptyList()) // loaded from DB by IdentityProvider
                    .build();

        } catch (Exception e) {
            log.error("Failed to extract AuthUser from JWT: {}", e.getMessage(), e);
            return null;
        }
    }


    private List<String> extractRoles(Jwt jwt) {
        List<String> realmRoles = extractRealmRoles(jwt);
        if (!realmRoles.isEmpty()) return realmRoles;

        List<String> clientRoles = extractClientRoles(jwt);
        if (!clientRoles.isEmpty()) return clientRoles;

        log.debug("No roles found in realm_access or resource_access[{}]", keycloakClientId);
        return Collections.emptyList();
    }

    private Collection<GrantedAuthority> buildAuthorities(AuthUser user) {
        Stream<GrantedAuthority> roleAuthorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(ROLE_PREFIX + role));

        Stream<GrantedAuthority> permissionAuthorities = user.getPermissions().stream()
                .map(SimpleGrantedAuthority::new);

        Stream<GrantedAuthority> tenantAuthority = user.getTenantId() != null
                ? Stream.of(new SimpleGrantedAuthority(TENANT_PREFIX + user.getTenantId()))
                : Stream.empty();

        return Stream.of(roleAuthorities, permissionAuthorities, tenantAuthority)
                .flatMap(s -> s)
                .collect(Collectors.toList());
    }

    private List<String> extractRealmRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null) return Collections.emptyList();

        return toStringList(realmAccess.get("roles"));
    }

    private List<String> extractClientRoles(Jwt jwt) {
        Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
        if (resourceAccess == null || !resourceAccess.containsKey(keycloakClientId)) {
            return Collections.emptyList();
        }

        Object clientEntry = resourceAccess.get(keycloakClientId);
        if (!(clientEntry instanceof Map<?, ?> clientMap)) return Collections.emptyList();

        return toStringList(clientMap.get("roles"));
    }

    @SuppressWarnings("unchecked")
    private List<String> toStringList(Object value) {
        if (!(value instanceof List<?> list)) return Collections.emptyList();
        return list.stream().map(Object::toString).collect(Collectors.toList());
    }

    private AbstractAuthenticationToken anonymousToken(Jwt jwt) {
        return new JwtAuthenticationToken(jwt, Collections.emptyList(), "anonymous");
    }
}
