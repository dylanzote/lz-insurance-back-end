package com.lz_Insurance.security.keycloak.config;

import com.lz_Insurance.security.keycloak.model.KeycloakProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(KeycloakProperties.class)
@ConditionalOnProperty(prefix = "insurance.security.idp", name = "type", havingValue = "keycloak")
public class KeyCloakConfig {

    private final KeycloakProperties keycloakProperties;

    @Bean
    Keycloak keyCloak() {
        log.info("Initializing Keycloak admin client for realm: {}", keycloakProperties.getRealm());
        return KeycloakBuilder.builder()
                .serverUrl(keycloakProperties.getServerUrl())
                .realm(keycloakProperties.getRealm())
                .clientId(keycloakProperties.getClientId())
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .username(keycloakProperties.getUsername())
                .password(keycloakProperties.getPassword())
                .clientSecret(keycloakProperties.getClientSecret())
                .build();
    }
}
