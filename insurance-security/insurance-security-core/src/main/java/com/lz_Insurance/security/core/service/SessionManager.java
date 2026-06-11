package com.lz_Insurance.security.core.service;

import com.lz_Insurance.security.core.model.AuthUser;
import com.lz_Insurance.security.core.spi.UserSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;


/**
 * Session management service - tracks user sessions across devices
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionManager {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String USER_SESSIONS_KEY = "user:sessions:";
    private static final String SESSION_KEY = "session:";
    private static final String DEVICE_SESSIONS_KEY = "device:sessions:";
    private static final String TOKEN_SESSION_KEY = "token:session:";


    public void registerSession(AuthUser user, String token, UserSession session) {
        String sessionId = session.getId();

        // Store session by ID
        String sessionKey = SESSION_KEY + sessionId;
        redisTemplate.opsForHash().putAll(sessionKey, toMap(session));
        redisTemplate.expire(sessionKey, Duration.ofHours(24));

        // Map token to session
        String tokenKey = TOKEN_SESSION_KEY + token;
        redisTemplate.opsForValue().set(tokenKey, sessionId, Duration.ofHours(24));

        // Add to user's sessions list
        String userSessionsKey = USER_SESSIONS_KEY + user.getUserId();
        redisTemplate.opsForSet().add(userSessionsKey, sessionId);
        redisTemplate.expire(userSessionsKey, Duration.ofDays(30));

        // Add to device sessions
        if (session.getDeviceId() != null) {
            String deviceSessionsKey = DEVICE_SESSIONS_KEY + session.getDeviceId();
            redisTemplate.opsForSet().add(deviceSessionsKey, sessionId);
            redisTemplate.expire(deviceSessionsKey, Duration.ofDays(30));
        }

        log.info("Session registered: {} for user: {}, device: {}", sessionId, user.getUserId(), session.getDeviceType());
    }

    public Optional<UserSession> getSessionByToken(String token) {
        String tokenKey = TOKEN_SESSION_KEY + token;
        String sessionId = (String) redisTemplate.opsForValue().get(tokenKey);

        if (sessionId == null) {
            return Optional.empty();
        }

        return getSessionById(sessionId);
    }

    /**
     * Get session by ID
     */
    public Optional<UserSession> getSessionById(String sessionId) {
        String sessionKey = SESSION_KEY + sessionId;
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(sessionKey);

        if (entries.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(fromMap(entries));
    }

    /**
     * Update session last access time
     */
    public void updateSessionAccess(String sessionId) {
        String sessionKey = SESSION_KEY + sessionId;
        redisTemplate.opsForHash().put(sessionKey, "lastAccessedAt", LocalDateTime.now().toString());
        redisTemplate.expire(sessionKey, Duration.ofHours(24));
    }

    /**
     * Get all sessions for a user
     */
    public List<UserSession> getUserSessions(String userId) {
        String userSessionsKey = USER_SESSIONS_KEY + userId;
        Set<Object> sessionIds = redisTemplate.opsForSet().members(userSessionsKey);

        if (sessionIds == null || sessionIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<UserSession> sessions = new ArrayList<>();
        for (Object sessionIdObj : sessionIds) {
            String sessionId = sessionIdObj.toString();
            getSessionById(sessionId).ifPresent(sessions::add);
        }

        return sessions;
    }

    /**
     * Get active session count for a user
     */
    public int getActiveSessionCount(String userId) {
        return getUserSessions(userId).size();
    }

    /**
     * Revoke a specific session
     */
    public void revokeSession(String sessionId) {
        Optional<UserSession> sessionOpt = getSessionById(sessionId);
        if (sessionOpt.isEmpty()) {
            return;
        }

        UserSession session = sessionOpt.get();

        // Remove from user's sessions
        String userSessionsKey = USER_SESSIONS_KEY + session.getUserId();
        redisTemplate.opsForSet().remove(userSessionsKey, sessionId);

        // Remove from device sessions
        if (session.getDeviceId() != null) {
            String deviceSessionsKey = DEVICE_SESSIONS_KEY + session.getDeviceId();
            redisTemplate.opsForSet().remove(deviceSessionsKey, sessionId);
        }

        // Remove token mapping (we need to find which token maps to this session)
        // This is done via token cleanup

        // Delete session data
        String sessionKey = SESSION_KEY + sessionId;
        redisTemplate.delete(sessionKey);

        log.info("Session revoked: {} for user: {}", sessionId, session.getUserId());
    }

    /**
     * Revoke all sessions for a user except current
     */
    public void revokeAllOtherSessions(String userId, String currentSessionId) {
        List<UserSession> sessions = getUserSessions(userId);

        for (UserSession session : sessions) {
            if (!session.getId().equals(currentSessionId)) {
                revokeSession(session.getId());
            }
        }

        log.info("Revoked all other sessions for user: {}, except: {}", userId, currentSessionId);
    }

    /**
     * Revoke all sessions for a user
     */
    public void revokeAllSessions(String userId) {
        List<UserSession> sessions = getUserSessions(userId);

        for (UserSession session : sessions) {
            revokeSession(session.getId());
        }

        log.info("Revoked all sessions for user: {}", userId);
    }

    /**
     * Revoke sessions by device
     */
    public void revokeDeviceSessions(String deviceId) {
        String deviceSessionsKey = DEVICE_SESSIONS_KEY + deviceId;
        Set<Object> sessionIds = redisTemplate.opsForSet().members(deviceSessionsKey);

        if (sessionIds != null) {
            for (Object sessionIdObj : sessionIds) {
                revokeSession(sessionIdObj.toString());
            }
        }

        redisTemplate.delete(deviceSessionsKey);
        log.info("Revoked all sessions for device: {}", deviceId);
    }

    /**
     * Clean up expired sessions (called by scheduled job)
     */
    public void cleanupExpiredSessions() {
        // Redis handles TTL automatically, but we can clean up references
        // This is a placeholder for any additional cleanup logic
        log.debug("Session cleanup completed");
    }

    private Map<String, String> toMap(UserSession session) {
        Map<String, String> map = new HashMap<>();
        map.put("id", session.getId());
        map.put("userId", session.getUserId());
        map.put("username", session.getUsername());
        map.put("deviceId", session.getDeviceId());
        map.put("deviceType", session.getDeviceType());
        map.put("deviceName", session.getDeviceName());
        map.put("ipAddress", session.getIpAddress());
        map.put("userAgent", session.getUserAgent());
        map.put("location", session.getLocation());
        map.put("startedAt", session.getStartedAt().toString());
        map.put("lastAccessedAt", session.getLastAccessedAt().toString());
        map.put("isCurrent", String.valueOf(session.isCurrent()));
        if (session.getClients() != null) {
            map.put("clients", session.getClients().toString());
        }
        return map;
    }

    private UserSession fromMap(Map<Object, Object> map) {
        return UserSession.builder()
            .id((String) map.get("id"))
            .userId((String) map.get("userId"))
            .username((String) map.get("username"))
            .deviceId((String) map.get("deviceId"))
            .deviceType((String) map.get("deviceType"))
            .deviceName((String) map.get("deviceName"))
            .ipAddress((String) map.get("ipAddress"))
            .userAgent((String) map.get("userAgent"))
            .location((String) map.get("location"))
            .startedAt(LocalDateTime.parse((String) map.get("startedAt")))
            .lastAccessedAt(LocalDateTime.parse((String) map.get("lastAccessedAt")))
            .isCurrent(Boolean.parseBoolean((String) map.get("isCurrent")))
            .build();
    }
}
