package com.lz_insurance.insurance.identity.infrastructure.config;

import com.lz_insurance.core.request.HttpService;
import com.lz_insurance.security.core.spi.IdentityProvider;
import com.lz_insurance.security.keycloak.KeycloakIdentityProvider;
import com.lz_insurance.security.keycloak.config.KeyCloakConfig;
import com.lz_insurance.security.keycloak.mapper.KeyCloakUserMapper;
import com.lz_insurance.security.keycloak.model.KeycloakProperties;
import com.lz_insurance.security.keycloak.service.KeyCloakService;
import org.keycloak.admin.client.Keycloak;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Wires the Keycloak identity-provider chain from the shared {@code insurance-security-keycloak}
 * module into the identity application context.
 *
 * <p><b>Why explicit wiring (not a broadened component scan):</b> the provider classes live in
 * {@code com.lz_insurance.security.keycloak.*}, outside this app's {@code com.lz_insurance.insurance}
 * scan root. Broadening the scan would drag in the rest of the (still unconfigured) security stack —
 * the same reasoning that keeps {@code JpaAuditingConfig} on a targeted {@code @Import} (G-009). So
 * the chain is declared here as explicit beans: {@link KeyCloakConfig} (imported) supplies the admin
 * {@link Keycloak} client (CLIENT_CREDENTIALS grant against {@code keycloak.realm}); this class adds
 * the mapper, {@link KeyCloakService}, and the {@link IdentityProvider} the
 * {@code KeycloakUserAdapter} consumes.
 *
 * <p><b>Gating:</b> like the adapter, everything here is gated on
 * {@code insurance.security.idp.type=keycloak}, so the chain stays inert unless the provider is
 * configured. {@link HttpService} is used only by {@code authenticate}/{@code refreshToken} (M3
 * login), never by the M2 provisioning path, so it is injected only if a bean is present.
 */
@Configuration
@ConditionalOnProperty(prefix = "insurance.security.idp", name = "type", havingValue = "keycloak")
@Import(KeyCloakConfig.class)
public class KeycloakAdapterConfig {

    @Bean
    KeyCloakUserMapper keyCloakUserMapper() {
        return new KeyCloakUserMapper();
    }

    @Bean
    KeyCloakService keyCloakService(Keycloak keycloak, KeycloakProperties keycloakProperties) {
        return new KeyCloakService(keycloak, keycloakProperties);
    }

    @Bean
    IdentityProvider identityProvider(KeyCloakService keyCloakService,
                                      ObjectProvider<HttpService> httpService,
                                      KeycloakProperties keycloakProperties,
                                      KeyCloakUserMapper keyCloakUserMapper) {
        return new KeycloakIdentityProvider(
                keyCloakService, httpService.getIfAvailable(), keycloakProperties, keyCloakUserMapper);
    }
}
