package com.lz_Insurance.security.core.web.filter;

import com.lz_Insurance.security.core.service.SessionManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Validates that the session associated with the JWT token is still active
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SessionValidationFilter extends OncePerRequestFilter {

    private final SessionManager sessionManager;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

         var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = (Jwt) jwtAuth.getCredentials();
            String token = jwt.getTokenValue();

            // Get session from token
            var sessionOpt = sessionManager.getSessionByToken(token);

            if (sessionOpt.isEmpty()) {
                log.warn("No active session found for token");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Session expired or invalid");
                return;
            }

            // Update last access time
            sessionManager.updateSessionAccess(sessionOpt.get().getId());
        }

        filterChain.doFilter(request, response);
    }
}

