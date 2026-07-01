# LZ Insurance — User Journeys

> Load in Claude Code with: `@docs/user-journeys.md`
> This file defines the INTENDED user experience for key flows.
> Implementation decisions must be derived from these journeys — never the other way around.

---

## Core Architectural Principle — Keycloak's Exact Role

**Keycloak does exactly two things:**
1. **Credential storage** — stores passwords (hashed) and issues password-set confirmations
2. **Token issuance** — validates credentials and issues JWTs

**Everything else is our system's responsibility:**
- Password policy enforcement (complexity, length, special chars) → our domain validates
  before calling Keycloak. Keycloak receives a valid password; it never rejects on policy.
- Password reuse prevention → our system maintains password history
- Password expiry → our domain tracks `passwordLastChangedAt` and enforces the window
- Account lockout → our system tracks failed login attempts; our domain locks the profile
- MFA orchestration → our system challenges and verifies; Keycloak issues the final token
- Session limits / concurrent session control → our Redis + session_registry
- First-login forced change → `IdentityProfile.passwordChangeRequired` (domain flag)

**Why:** This keeps every business rule in one place — our domain. If Keycloak is replaced
tomorrow, no business rule is lost. Keycloak is a commodity; our business logic is the asset.

Never configure business rules in Keycloak realm settings. Keep Keycloak as a dumb credential
store and token issuer.

---

## Authentication Philosophy

LZ Insurance is an enterprise insurance platform used by internal staff (agents, underwriters,
adjusters, branch managers, admins) who log in frequently — daily or multiple times per day.

**Decision: password-based authentication with MFA, NOT magic links.**
Magic links are best for infrequent logins and consumer SaaS. Enterprise financial platforms
with sensitive regulatory requirements and frequent daily usage require password + MFA.
Magic links in enterprise environments are also vulnerable to corporate mail scanners
pre-clicking the link before the user can, invalidating the token.

---

## Password Policy (enforced by OUR system, before any Keycloak call)

Our domain validates ALL of the following before calling `KeycloakUserPort.changePassword()`.
Keycloak receives only already-validated passwords:

- Minimum 12 characters
- At least 1 uppercase, 1 lowercase, 1 digit, 1 special character
- Cannot reuse the last 5 passwords (our system stores hashed password history)
- Expires every 90 days for internal roles (our domain tracks `passwordLastChangedAt`)
- Lockout after 5 failed login attempts (our domain tracks attempts, locks `IdentityProfile`)

These are domain rules, not Keycloak realm settings. Do NOT configure them in Keycloak.

**Domain models needed (future milestones):**
- `PasswordHistory` — stores hashed previous passwords per identity profile (for reuse check)
- `LoginAttempt` — tracks failed attempts per identity profile (for lockout — M6)

---

## UJ-001 — Tenant Bootstrap (First Admin Onboarding)

**Actor:** Platform operator (deployment pipeline or LZ Insurance ops team)
**Trigger:** New insurance company deploys LZ Insurance platform for the first time

```
1. Operator calls POST /internal/bootstrap with:
   { tenantId, branchId (headquarter), adminEmail, firstName, lastName, temporaryPassword }
   Header: X-Bootstrap-Secret: [env var]

   Note: operator provides temporaryPassword. Our system validates it meets password policy
   BEFORE calling Keycloak. Keycloak never sees an invalid password.

2. System validates:
   - Bootstrap secret correct → 403 if wrong
   - Tenant exists and is NOT already bootstrapped → 409 if already bootstrapped
   - temporaryPassword meets password policy → 400 if not

3. System creates TENANT_ADMIN IdentityProfile:
   status = PENDING_APPROVAL (transitions to ACTIVE at step 5)

4. System calls Keycloak:
   - createUser(email, temporaryPassword) → Keycloak account created WITH temp password
     (password was already validated in step 2 — Keycloak just stores the valid credential)
   - assignRealmRole(userId, "TENANT_ADMIN")

   Note: createUser already sets the password. Do NOT call changePassword() again with
   the same temporaryPassword — that is redundant. The only changePassword() call happens
   in step 13 when the user sets their NEW password.

5. System:
   - profile.activate(keycloakUserId) → status=ACTIVE, passwordChangeRequired=TRUE
   - tenant.markBootstrapped(branchId) → bootstrapped=true

6. Notification service (G-014 — deferred until insurance-notification service is built):
   Emits TenantBootstrapped domain event.
   Notification service sends welcome email to adminEmail containing the temporaryPassword.
   Until G-014 is resolved: temporaryPassword returned in the API response (dev/test only).

7. First admin receives email, navigates to login screen.

8. First admin enters email + temporaryPassword → Keycloak validates → issues JWT.

9. Our system (M3 login flow):
   - Validates JWT
   - Loads IdentityProfile for the keycloakUserId in the token claims
   - Checks passwordChangeRequired == TRUE
   - Does NOT issue a full application session
   - Returns PASSWORD_CHANGE_REQUIRED status to frontend

10. Frontend redirects admin to forced password change screen.

11. Admin enters: [new password] + [confirm new password]

12. Our system validates (ALL before touching Keycloak):
    - New password meets complexity policy (12+ chars, uppercase, lowercase, digit, special)
    - New password is not in PasswordHistory (reuse check) — simplified for bootstrap
      since no history exists yet
    - MFA challenge if required for TENANT_ADMIN role (M6)

13. Our system:
    - Calls KeycloakUserPort.changePassword(keycloakUserId, newPassword, temporary=false)
      (Keycloak receives the already-validated new password — just stores it)
    - Calls profile.completePasswordChange() → passwordChangeRequired=FALSE
    - Issues full application session

14. Admin lands on internal dashboard.
15. Admin creates branches, invites staff via normal flows.
```

---

## UJ-002 — Internal Staff Invitation & Onboarding

**Actor:** TENANT_ADMIN or BRANCH_MANAGER
**Trigger:** Admin wants to add a new staff member

```
1. Admin calls POST /api/v1/users:
   { email, firstName, lastName, internalUserType, branchId }

2. System validates:
   - Caller has user:create permission at appropriate scope
   - No existing active profile with same email in this tenant → 409 if duplicate

3. System creates IdentityProfile:
   - status = PENDING_APPROVAL
   - NO Keycloak account yet (Keycloak is never called before approval)
   - Creates ApprovalRequest (status=PENDING)

4. Notification (G-014 — deferred):
   Branch manager notified of pending approval.

5. BRANCH_MANAGER calls POST /api/v1/approvals/{id}/approve

6. System (ApproveRegistration use case):
   - Generates a secure temporary password (our system generates it, applies policy)
   - Our system validates the generated password meets policy (it generates it, so it does)
   - Calls Keycloak: createUser(email, temporaryPassword)
   - Calls Keycloak: assignRealmRole(userId, role)
   - profile.activate(keycloakUserId) → status=ACTIVE, passwordChangeRequired=TRUE
   - ApprovalRequest → APPROVED

7. Notification (G-014 — deferred):
   New staff member notified with temporary password.

8-13. Same as UJ-001 steps 7-13 (first login → forced password change → session).
```

**Key rule:** Keycloak account created ONLY on approval, NEVER before. This is a domain
invariant. The `createUser` call happens exactly once per profile, in the approval step.

---

## UJ-003 — Branch Manager Rejection

```
1. BRANCH_MANAGER calls POST /api/v1/approvals/{id}/reject with { reviewNotes }
2. System:
   - ApprovalRequest → REJECTED
   - IdentityProfile remains PENDING_APPROVAL (or transitions to a REJECTED terminal state)
   - NO Keycloak account is ever created
3. Notification (G-014 — deferred): requester notified with rejection reason.
```

---

## UJ-004 — Staff Password Change (Subsequent Changes, Post First-Login)

```
1. Staff member is logged in with a full session (passwordChangeRequired=false).
2. Staff navigates to Account Settings → Change Password.
3. Staff provides: [current password] + [new password] + [confirm new password]

4. Our system validates (ALL before touching Keycloak):
   a. Current password verification: call Keycloak authenticate(email, currentPassword)
      to confirm the current credential is valid. This is authentication — Keycloak's job.
   b. New password meets complexity policy (12+ chars etc.)
   c. New password not in PasswordHistory (last 5 hashes) — reuse prevention
   d. MFA challenge if required for this role

5. All validations pass → our system:
   a. Calls KeycloakUserPort.changePassword(keycloakUserId, newPassword, temporary=false)
   b. Saves new hashed password to PasswordHistory
   c. Updates profile.passwordLastChangedAt = now()

6. Session continues unchanged.
```

---

## UJ-005 — Account Lockout (M6)

```
1. System tracks failed login attempts in LoginAttempt domain model (our system, not Keycloak).
2. After 5 consecutive failed attempts within a configurable window:
   - Our system calls profile.lock() → IdentityStatus.LOCKED
   - Our system calls KeycloakUserPort.updateUser(userId, enabled=false) (Keycloak account
     disabled as a safety net, but our system's LOCKED flag is authoritative)
3. Admin can unlock via PUT /api/v1/users/{id}/unlock
4. Lockout duration is configurable per tenant via tenant settings (future — M6).
```

---

## UJ-006 — External Customer Self-Registration (M4 — deferred)

External realm, self-service flow. Separate user journey document at M4 design time.

---

## Authentication Methods by Actor Type

| Actor | Login Method | MFA Required | Notes |
|---|---|---|---|
| TENANT_ADMIN | Password + MFA | Yes (M6) | Highest privilege |
| BRANCH_MANAGER | Password + MFA | Yes (M6) | Branch-level admin |
| UNDERWRITER | Password | Recommended | Policy approval |
| CLAIMS_ADJUSTER | Password | Recommended | Claims handling |
| AGENT | Password | Optional | Front-line sales |
| STAFF | Password | Optional | Read-only baseline |
| POLICYHOLDER | Password | Optional | External realm, M4 |
| CLAIMANT | Password | Optional | External realm, M4 |

Note: MFA is orchestrated by OUR system (challenge, verify). Keycloak issues the token
only after our system has completed all required verification steps.

---

## Reference for Claude Code

When implementing any authentication or password flow:
1. Read this file first
2. Our system validates EVERYTHING before calling Keycloak
3. Keycloak receives only already-validated input
4. Keycloak is never the source of a business rule rejection
5. All user journey steps that say "our system validates" happen in domain use cases,
   with no Spring/framework coupling
