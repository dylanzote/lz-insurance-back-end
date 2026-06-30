package com.lz_insurance.security.keycloak.model;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Data
@ConfigurationProperties(prefix = "keycloak")
public class KeycloakProperties {
    private String serverUrl;
    private String realm;
    private String clientId;
    private String grantType;
    private String username;
    private String password;
    private String clientSecret;
    private String authServerUrl;
    private int connectionTimeout = 10;
    private int socketTimeout = 10;
    private String scope = "openid email profile";

    public String getAuthServerUrl() {
        if (authServerUrl == null) {
            return serverUrl + "/realms/" + realm + "/protocol/openid-connect/token";
        }
        return authServerUrl;
    }
}
