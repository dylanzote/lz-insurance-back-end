package com.lz_Insurance.insurance.identity.domain.model;

import com.lz_Insurance.core.model.BaseDomainEntity;
import com.lz_Insurance.insurance.identity.domain.enumeration.ActorType;
import com.lz_Insurance.insurance.identity.domain.enumeration.ExternalUserType;
import com.lz_Insurance.insurance.identity.domain.enumeration.IdentityStatus;
import com.lz_Insurance.insurance.identity.domain.enumeration.InternalUserType;
import com.lz_Insurance.insurance.identity.domain.exception.InvalidStateTransitionException;
import com.lz_Insurance.insurance.identity.domain.support.DomainGuard;
import lombok.Getter;
import lombok.ToString;

/**
 * Unified identity for both internal staff and external customers, differentiated
 * by {@link ActorType}. This is the aggregate root for identity — all identity
 * operations flow through it.
 *
 * <p>Invariants enforced at construction:
 * <ul>
 *   <li>INTERNAL profiles carry an {@code internalUserType} and a {@code branchId};
 *       {@code externalUserType} must be null.</li>
 *   <li>EXTERNAL profiles carry an {@code externalUserType}; {@code internalUserType}
 *       must be null and {@code branchId} is optional.</li>
 * </ul>
 *
 * <p>Lifecycle: a profile is born PENDING_APPROVAL with no Keycloak account. It is
 * {@link #activate(String) activated} (Keycloak user attached) only after approval,
 * then may be {@link #suspend() suspended} / {@link #deactivate() deactivated}.
 */
@Getter
@ToString(callSuper = true)
public class IdentityProfile extends BaseDomainEntity {

    private final String tenantId;
    private final String branchId;
    private final ActorType actorType;
    private final InternalUserType internalUserType;
    private final ExternalUserType externalUserType;
    private final String email;
    private final String firstName;
    private final String lastName;

    private String keycloakUserId;
    private IdentityStatus status;

    private IdentityProfile(String tenantId, String branchId, ActorType actorType,
                            InternalUserType internalUserType, ExternalUserType externalUserType,
                            String email, String firstName, String lastName) {
        DomainGuard.notBlank(tenantId, "tenantId");
        DomainGuard.notNull(actorType, "actorType");
        DomainGuard.notBlank(email, "email");
        DomainGuard.notBlank(firstName, "firstName");
        DomainGuard.notBlank(lastName, "lastName");

        if (actorType == ActorType.INTERNAL) {
            DomainGuard.notNull(internalUserType, "internalUserType");
            DomainGuard.isNull(externalUserType, "externalUserType",
                    "externalUserType must be null for an INTERNAL profile");
            DomainGuard.notBlank(branchId, "branchId");
        } else {
            DomainGuard.notNull(externalUserType, "externalUserType");
            DomainGuard.isNull(internalUserType, "internalUserType",
                    "internalUserType must be null for an EXTERNAL profile");
        }

        this.tenantId = tenantId;
        this.branchId = branchId;
        this.actorType = actorType;
        this.internalUserType = internalUserType;
        this.externalUserType = externalUserType;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.status = IdentityStatus.PENDING_APPROVAL;
    }

    /** Factory for an internal staff member, scoped to a branch. */
    public static IdentityProfile internal(String tenantId, String branchId, InternalUserType type,
                                           String email, String firstName, String lastName) {
        return new IdentityProfile(tenantId, branchId, ActorType.INTERNAL, type, null,
                email, firstName, lastName);
    }

    /** Factory for an external customer; branch is optional. */
    public static IdentityProfile external(String tenantId, String branchId, ExternalUserType type,
                                           String email, String firstName, String lastName) {
        return new IdentityProfile(tenantId, branchId, ActorType.EXTERNAL, null, type,
                email, firstName, lastName);
    }

    /**
     * Attaches the Keycloak account and moves the profile live.
     * Valid only from PENDING_APPROVAL — a Keycloak user is never created before
     * approval, so re-activation of an already-active profile is illegal.
     */
    public void activate(String keycloakUserId) {
        DomainGuard.notBlank(keycloakUserId, "keycloakUserId");
        requireStatus(IdentityStatus.PENDING_APPROVAL, IdentityStatus.ACTIVE);
        this.keycloakUserId = keycloakUserId;
        this.status = IdentityStatus.ACTIVE;
    }

    /** ACTIVE -> SUSPENDED. */
    public void suspend() {
        requireStatus(IdentityStatus.ACTIVE, IdentityStatus.SUSPENDED);
        this.status = IdentityStatus.SUSPENDED;
    }

    /** SUSPENDED -> ACTIVE. */
    public void reactivate() {
        requireStatus(IdentityStatus.SUSPENDED, IdentityStatus.ACTIVE);
        this.status = IdentityStatus.ACTIVE;
    }

    /** ACTIVE or SUSPENDED -> DEACTIVATED (terminal). */
    public void deactivate() {
        if (status != IdentityStatus.ACTIVE && status != IdentityStatus.SUSPENDED) {
            throw new InvalidStateTransitionException(status, IdentityStatus.DEACTIVATED, "IdentityProfile");
        }
        this.status = IdentityStatus.DEACTIVATED;
    }

    private void requireStatus(IdentityStatus required, IdentityStatus target) {
        if (status != required) {
            throw new InvalidStateTransitionException(status, target, "IdentityProfile");
        }
    }
}
