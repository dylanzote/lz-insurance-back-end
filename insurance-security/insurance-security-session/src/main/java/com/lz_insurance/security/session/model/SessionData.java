package com.lz_insurance.security.session.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionData implements Serializable {

    private String sessionId;
    private String userId;
    private String username;
    private String tenantId;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime createdAt;
    private LocalDateTime lastAccessedAt;
    private LocalDateTime expiresAt;
    private Map<String, Object> attributes;
    private boolean active;

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public void refreshLastAccessed() {
        this.lastAccessedAt = LocalDateTime.now();
    }
}
