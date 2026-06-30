package com.lz_insurance.insurance.identity.domain.model;

import com.lz_insurance.insurance.identity.domain.enumeration.SessionStatus;
import com.lz_insurance.insurance.identity.domain.exception.DomainValidationException;
import com.lz_insurance.insurance.identity.domain.exception.InvalidStateTransitionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SessionRegistryTest {

    private SessionRegistry activeSession(LocalDateTime expiresAt) {
        return new SessionRegistry("profile-1", "tenant-1", "hashed-token",
                "10.0.0.1", "junit-agent", expiresAt);
    }

    @Test
    @DisplayName("a new session is ACTIVE and valid when expiry is in the future")
    void newSessionIsValid() {
        SessionRegistry session = activeSession(LocalDateTime.now().plusHours(1));

        assertThat(session.getStatus()).isEqualTo(SessionStatus.ACTIVE);
        assertThat(session.isValid()).isTrue();
        assertThat(session.getLastActivityAt()).isNotNull();
    }

    @Test
    @DisplayName("blank session token is rejected")
    void blankTokenRejected() {
        assertThatThrownBy(() -> new SessionRegistry("profile-1", "tenant-1", " ",
                "10.0.0.1", "agent", LocalDateTime.now().plusHours(1)))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    @DisplayName("an ACTIVE but past-expiry session is not valid")
    void expiredByTimeIsNotValid() {
        SessionRegistry session = activeSession(LocalDateTime.now().minusSeconds(1));

        assertThat(session.getStatus()).isEqualTo(SessionStatus.ACTIVE);
        assertThat(session.isValid()).isFalse();
    }

    @Test
    @DisplayName("revoke moves ACTIVE -> REVOKED and invalidates the session")
    void revoke() {
        SessionRegistry session = activeSession(LocalDateTime.now().plusHours(1));

        session.revoke();

        assertThat(session.getStatus()).isEqualTo(SessionStatus.REVOKED);
        assertThat(session.isValid()).isFalse();
    }

    @Test
    @DisplayName("expire moves ACTIVE -> EXPIRED")
    void expire() {
        SessionRegistry session = activeSession(LocalDateTime.now().plusHours(1));

        session.expire();

        assertThat(session.getStatus()).isEqualTo(SessionStatus.EXPIRED);
        assertThat(session.isValid()).isFalse();
    }

    @Test
    @DisplayName("a terminal session cannot be revoked again")
    void cannotRevokeTerminalSession() {
        SessionRegistry session = activeSession(LocalDateTime.now().plusHours(1));
        session.revoke();

        assertThatThrownBy(session::revoke).isInstanceOf(InvalidStateTransitionException.class);
        assertThatThrownBy(session::expire).isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    @DisplayName("touch advances lastActivityAt only while ACTIVE")
    void touchOnlyWhileActive() {
        SessionRegistry session = activeSession(LocalDateTime.now().plusHours(1));
        LocalDateTime before = session.getLastActivityAt();

        session.touch();
        assertThat(session.getLastActivityAt()).isAfterOrEqualTo(before);

        session.revoke();
        LocalDateTime afterRevoke = session.getLastActivityAt();
        session.touch();
        assertThat(session.getLastActivityAt()).isEqualTo(afterRevoke);
    }
}
