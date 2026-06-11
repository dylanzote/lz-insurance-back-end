package com.lz_Insurance.security.session.service;

import com.lz_Insurance.security.session.model.SessionData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisSessionService {

    private static final String SESSION_PREFIX = "session:";
    private static final Duration DEFAULT_TTL = Duration.ofHours(8);

    private final RedisTemplate<String, Object> redisTemplate;

    public void createSession(SessionData sessionData) {
        String key = getSessionKey(sessionData.getSessionId());
        redisTemplate.opsForValue().set(key, sessionData, DEFAULT_TTL);
        log.debug("Created session: {}", sessionData.getSessionId());
    }

    public SessionData getSession(String sessionId) {
        String key = getSessionKey(sessionId);
        SessionData sessionData = (SessionData) redisTemplate.opsForValue().get(key);
        
        if (sessionData != null) {
            sessionData.refreshLastAccessed();
            redisTemplate.opsForValue().set(key, sessionData, DEFAULT_TTL);
        }
        
        return sessionData;
    }

    public void updateSession(SessionData sessionData) {
        String key = getSessionKey(sessionData.getSessionId());
        redisTemplate.opsForValue().set(key, sessionData, DEFAULT_TTL);
        log.debug("Updated session: {}", sessionData.getSessionId());
    }

    public void deleteSession(String sessionId) {
        String key = getSessionKey(sessionId);
        redisTemplate.delete(key);
        log.debug("Deleted session: {}", sessionId);
    }

    public void deleteAllUserSessions(String userId) {
        // This would require a scan operation in production
        // For now, we'll implement a simple version
        log.warn("deleteAllUserSessions not fully implemented - requires Redis scan");
    }

    public boolean isSessionValid(String sessionId) {
        SessionData sessionData = getSession(sessionId);
        return sessionData != null && sessionData.isActive() && !sessionData.isExpired();
    }

    private String getSessionKey(String sessionId) {
        return SESSION_PREFIX + sessionId;
    }
}
