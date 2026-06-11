package com.lz_Insurance.security.core.spi;

import com.lz_Insurance.security.core.model.AuthUser;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private int expiresIn;
    private int refreshExpiresIn;
    private String sessionState;
    private String sessionId;
    private AuthUser user;
}
