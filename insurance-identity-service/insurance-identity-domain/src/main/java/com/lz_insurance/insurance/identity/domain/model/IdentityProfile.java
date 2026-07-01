package com.lz_insurance.insurance.identity.domain.model;

import com.lz_insurance.core.model.BaseDomainEntity;
import com.lz_insurance.insurance.identity.domain.enums.ActorType;
import com.lz_insurance.insurance.identity.domain.enums.ExternalUserType;
import com.lz_insurance.insurance.identity.domain.enums.IdentityStatus;
import com.lz_insurance.insurance.identity.domain.enums.InternalUserType;
import com.lz_insurance.insurance.identity.domain.exception.InvalidStateTransitionException;
import com.lz_insurance.insurance.identity.domain.support.DomainGuard;
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
 *
 * <p>First-login password change is enforced by the domain, NOT the identity provider:
 * {@link #activate(String)} raises {@link #passwordChangeRequired}, and only a successful
 * {@link #completePasswordChange()} through our own flow clears it. Login (M3) must withhold a full
 * session while the flag is set. This keeps enforcement provider-independent — swapping identity
 * providers cannot silently drop it.
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
    private boolean passwordChangeRequired;

    /**
     * BCrypt hash of the current credential — OUR system's authoritative copy (Keycloak keeps its
     * own for authentication). Set whenever the password changes: the generated temporary credential
     * at bootstrap/approval, then again at forced/subsequent change. Never holds a raw password.
     */
    private String currentPasswordHash;

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

    /**
     * Reconstitution constructor for the persistence layer: restores the full stored
     * state (including {@code keycloakUserId} and any non-PENDING status). Enforces the
     * structural INTERNAL/EXTERNAL field invariants but does NOT force the
     * PENDING_APPROVAL creation state.
     */
    private IdentityProfile(String tenantId, String branchId, ActorType actorType,
                            InternalUserType internalUserType, ExternalUserType externalUserType,
                            String email, String firstName, String lastName,
                            String keycloakUserId, IdentityStatus status,
                            boolean passwordChangeRequired, String currentPasswordHash) {
        DomainGuard.notBlank(tenantId, "tenantId");
        DomainGuard.notNull(actorType, "actorType");
        DomainGuard.notBlank(email, "email");
        DomainGuard.notBlank(firstName, "firstName");
        DomainGuard.notBlank(lastName, "lastName");
        DomainGuard.notNull(status, "status");

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
        this.keycloakUserId = keycloakUserId;
        this.status = status;
        this.passwordChangeRequired = passwordChangeRequired;
        this.currentPasswordHash = currentPasswordHash;
    }

    /** Factory for an internal staff member, scoped to a branch. */
    public static IdentityProfile internal(String tenantId, String branchId, InternalUserType type, String email, String firstName, String lastName) {
        return new IdentityProfile(tenantId, branchId, ActorType.INTERNAL, type, null, email, firstName, lastName);
    }

    /** Factory for an external customer; branch is optional. */
    public static IdentityProfile external(String tenantId, String branchId, ExternalUserType type,
                                           String email, String firstName, String lastName) {
        return new IdentityProfile(tenantId, branchId, ActorType.EXTERNAL, null, type,
                email, firstName, lastName);
    }

    /**
     * Rebuilds an {@code IdentityProfile} from its persisted state (infrastructure mapper only).
     * Base/audit fields are restored separately via the inherited setters.
     */
    public static IdentityProfile reconstitute(String tenantId, String branchId, ActorType actorType,
                                               InternalUserType internalUserType,
                                               ExternalUserType externalUserType, String email,
                                               String firstName, String lastName,
                                               String keycloakUserId, IdentityStatus status,
                                               boolean passwordChangeRequired, String currentPasswordHash) {
        return new IdentityProfile(tenantId, branchId, actorType, internalUserType, externalUserType,
                email, firstName, lastName, keycloakUserId, status, passwordChangeRequired, currentPasswordHash);
    }

    /**
     * Records the hash of the current credential — OUR authoritative copy, updated in lockstep with
     * every password set (temporary at bootstrap/approval, then forced/subsequent change). The raw
     * password is hashed by the caller through the {@code PasswordHasher} port; only the hash arrives.
     */
    public void updatePasswordHash(String passwordHash) {
        DomainGuard.notBlank(passwordHash, "passwordHash");
        this.currentPasswordHash = passwordHash;
    }

    /**
     * Attaches the Keycloak account and moves the profile live.
     * Valid only from PENDING_APPROVAL — a Keycloak user is never created before
     * approval, so re-activation of an already-active profile is illegal.
     *
     * <p>Activation raises {@link #passwordChangeRequired}: a freshly activated staff member must
     * change their temporary credential through our own flow before receiving a full session. The
     * domain owns this rule; the identity provider is not trusted to enforce it.
     */
    public void activate(String keycloakUserId) {
        DomainGuard.notBlank(keycloakUserId, "keycloakUserId");
        requireStatus(IdentityStatus.PENDING_APPROVAL, IdentityStatus.ACTIVE);
        this.keycloakUserId = keycloakUserId;
        this.status = IdentityStatus.ACTIVE;
        this.passwordChangeRequired = true;
    }

    /**
     * Clears the forced-password-change flag once the user has successfully changed their credential
     * through our own password-change flow (which owns MFA/policy/current-password checks and only
     * then calls the identity provider). Valid only for an ACTIVE profile that still has a pending
     * change — calling it otherwise is an illegal transition.
     */
    public void completePasswordChange() {
        if (status != IdentityStatus.ACTIVE || !passwordChangeRequired) {
            throw new InvalidStateTransitionException(
                    "completePasswordChange requires an ACTIVE profile with a pending password change (status=%s, passwordChangeRequired=%s)"
                            .formatted(status, passwordChangeRequired));
        }
        this.passwordChangeRequired = false;
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
