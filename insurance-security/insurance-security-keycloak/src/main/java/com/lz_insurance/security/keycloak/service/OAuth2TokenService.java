package com.lz_insurance.security.keycloak.service;

import com.lz_insurance.core.exception.FunctionalException;
import com.lz_insurance.core.request.HttpService;
import com.lz_insurance.security.keycloak.model.KeycloakProperties;
import com.lz_insurance.security.keycloak.model.TokenData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.locks.ReentrantLock;

@Service
@Slf4j
@RequiredArgsConstructor
public class OAuth2TokenService {

    private static final int EXPIRY_BUFFER_SECONDS = 60;

    private final HttpService httpService;

    private final KeycloakProperties keycloakProperties;

    private final KeyCloakService keyCloakService;

    private String cachedToken;
    private LocalDateTime tokenExpiry;
    private final ReentrantLock tokenLock = new ReentrantLock();

    public String getAccessToken() {
        tokenLock.lock();
        try {
            if (isTokenValid()) {
                log.info("Using cached OAuth2 token");
                return cachedToken;
            }

            try {
                TokenData response = httpService.post(keycloakProperties.getAuthServerUrl(), keyCloakService.buildClientCredentialsRequest(), TokenData.class);
                if (response != null && response.getAccessToken() != null) {
                    cachedToken = response.getAccessToken();
                    tokenExpiry = LocalDateTime.now().plusSeconds(response.getExpiresIn() - EXPIRY_BUFFER_SECONDS);
                    log.info("Successfully obtained OAuth2 token, expires in {} seconds", response.getExpiresIn());
                    return cachedToken;
                }
                log.info("Invalid response while obtaining OAuth2 token: {}", response);
                throw new FunctionalException("Failed to obtain OAuth2 token: invalid response");

            } catch (Exception e) {
                log.error("Failed to obtain OAuth2 token from Keycloak: {}", e.getMessage(), e);
                throw new FunctionalException("Failed to obtain OAuth2 token");
            }
        } finally {
            tokenLock.unlock();
        }
    }

    /**
     * Forces the next call to getAccessToken() to request a fresh token.
     * Useful after a 401 response from a downstream service.
     */
    public void invalidateToken() {
        tokenLock.lock();
        try {
            cachedToken = null;
            tokenExpiry = null;
            log.debug("OAuth2 token cache invalidated");
        } finally {
            tokenLock.unlock();
        }
    }

    private boolean isTokenValid() {
        return cachedToken != null
                && tokenExpiry != null
                && tokenExpiry.isAfter(LocalDateTime.now());
    }

}

