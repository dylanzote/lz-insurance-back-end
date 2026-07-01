package com.lz_insurance.core.context;

import java.time.Instant;
import java.util.Objects;

/**
 * Immutable per-request context, populated once at the HTTP boundary and carried EXPLICITLY through
 * the call stack. Pure Java — no Spring, no servlet types — so the domain can accept it as a plain
 * parameter without coupling to infrastructure.
 *
 * <p>The four transport/tracing fields (correlationId, ipAddress, userAgent, requestedAt) are always
 * present. The identity fields (actorId, tenantId, sessionId) are null until M3 wires JWT extraction
 * into the filter; for bootstrap and system jobs there is no authenticated actor, so
 * {@link #resolvedActorId()} falls back to {@value #SYSTEM_ACTOR}.
 */
public record RequestContext(
        String correlationId,
        String ipAddress,
        String userAgent,
        Instant requestedAt,
        String actorId,
        String tenantId,
        String sessionId) {

    public static final String SYSTEM_ACTOR = "SYSTEM";

    public RequestContext {
        Objects.requireNonNull(correlationId, "correlationId");
        Objects.requireNonNull(ipAddress, "ipAddress");
        Objects.requireNonNull(userAgent, "userAgent");
        Objects.requireNonNull(requestedAt, "requestedAt");
    }

    /** True once a JWT-authenticated actor is present (M3+); false for bootstrap/system flows. */
    public boolean isAuthenticated() {
        return actorId != null;
    }

    /** Actor for audit attribution: the authenticated id, or {@value #SYSTEM_ACTOR} when there is none. */
    public String resolvedActorId() {
        return actorId != null ? actorId : SYSTEM_ACTOR;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Pure-Java builder — the filter sets the four always-present fields; the identity fields stay
     * null in M2 (populated from JWT claims once M3 wires token extraction into the filter).
     */
    public static final class Builder {
        private String correlationId;
        private String ipAddress;
        private String userAgent;
        private Instant requestedAt;
        private String actorId;
        private String tenantId;
        private String sessionId;

        public Builder correlationId(String correlationId) { this.correlationId = correlationId; return this; }
        public Builder ipAddress(String ipAddress)         { this.ipAddress = ipAddress; return this; }
        public Builder userAgent(String userAgent)         { this.userAgent = userAgent; return this; }
        public Builder requestedAt(Instant requestedAt)    { this.requestedAt = requestedAt; return this; }
        public Builder actorId(String actorId)             { this.actorId = actorId; return this; }
        public Builder tenantId(String tenantId)           { this.tenantId = tenantId; return this; }
        public Builder sessionId(String sessionId)         { this.sessionId = sessionId; return this; }

        public RequestContext build() {
            return new RequestContext(correlationId, ipAddress, userAgent, requestedAt,
                    actorId, tenantId, sessionId);
        }
    }
}
