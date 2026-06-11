package com.lz_Insurance.security.core.spi;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class UserSession {
    private String id;
    private String userId;
    private String username;
    private String deviceId;
    private String deviceType;
    private String deviceName;
    private String ipAddress;
    private String userAgent;
    private String location;
    private LocalDateTime startedAt;
    private LocalDateTime lastAccessedAt;
    private boolean isCurrent;
    private Map<String, String> clients;
}
