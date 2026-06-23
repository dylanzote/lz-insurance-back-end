package com.lz_Insurance.insurance.identity.domain.model;

import com.lz_Insurance.core.model.BaseDomainEntity;
import com.lz_Insurance.insurance.identity.domain.enumeration.SessionStatus;
import com.lz_Insurance.insurance.identity.domain.exception.InvalidStateTransitionException;
import com.lz_Insurance.insurance.identity.domain.support.DomainGuard;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Audit/admin record of an authenticated session (the authoritative store is Redis).
 * Holds a hashed token, never the raw value. A session is created ACTIVE and may be
 * {@link #revoke() revoked} or {@link #expire() expired} — both terminal.
 */
@Getter
@ToString(callSuper = true)
public class SessionRegistry extends BaseDomainEntity {

    private final String identityProfileId;
    private final String tenantId;
    private final String sessionToken;
    private final String ipAddress;
    private final String userAgent;
    private final LocalDateTime expiresAt;

    private SessionStatus status;
    private LocalDateTime lastActivityAt;

    public SessionRegistry(String identityProfileId, String tenantId, String sessionToken,
                           String ipAddress, String userAgent, LocalDateTime expiresAt) {
        DomainGuard.notBlank(identityProfileId, "identityProfileId");
        DomainGuard.notBlank(tenantId, "tenantId");
        DomainGuard.notBlank(sessionToken, "sessionToken");
        DomainGuard.notNull(expiresAt, "expiresAt");

        this.identityProfileId = identityProfileId;
        this.tenantId = tenantId;
        this.sessionToken = sessionToken;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.expiresAt = expiresAt;
        this.status = SessionStatus.ACTIVE;
        this.lastActivityAt = LocalDateTime.now();
    }

    /** ACTIVE -> REVOKED (manual / admin sign-out). */
    public void revoke() {
        requireActive(SessionStatus.REVOKED);
        this.status = SessionStatus.REVOKED;
    }

    /** ACTIVE -> EXPIRED (token lifetime elapsed). */
    public void expire() {
        requireActive(SessionStatus.EXPIRED);
        this.status = SessionStatus.EXPIRED;
    }

    /** Records activity on a live session; ignored once the session is terminal. */
    public void touch() {
        if (status == SessionStatus.ACTIVE) {
            this.lastActivityAt = LocalDateTime.now();
        }
    }

    /** A session is usable only while ACTIVE and before its expiry instant. */
    public boolean isValid() {
        return status == SessionStatus.ACTIVE && LocalDateTime.now().isBefore(expiresAt);
    }

    private void requireActive(SessionStatus target) {
        if (status != SessionStatus.ACTIVE) {
            throw new InvalidStateTransitionException(status, target, "SessionRegistry");
        }
    }
}
