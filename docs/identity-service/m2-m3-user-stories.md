# M2 + M3 User Stories — insurance-identity-service

> Load in Claude Code with: `@docs/identity-service/m2-m3-user-stories.md`
> Prereq: M1 complete (domain, persistence, repositories, boot verification all green).

---

## Architectural standards for M2/M3 (read first)

### Use-case layer — domain stays FRAMEWORK-FREE
- Use cases are plain Java classes in `insurance-identity-domain/domain/usecase/`.
- They implement an **in-port interface** from `domain/port/in/` (the contract the API calls).
- They depend on **out-port interfaces** (`domain/port/out/`) via constructor injection — NO Spring
  annotations, NO Spring imports, NO `@Service`/`@Component` in the domain.
- Wiring happens in infrastructure: a `@Configuration` class
  (`infrastructure/config/IdentityUseCaseConfig`) declares each use case as an `@Bean`, passing the
  real adapter implementations. The domain expresses needs; infrastructure satisfies them.
- This keeps the domain portable, unit-testable without Spring, and reusable outside this stack.

### Logging
- `@Slf4j` (Lombok) on use cases and boundary points — NOT on adapters/mappers/entities.
- INFO for business events, WARN/ERROR for failures, DEBUG for diagnostics.
- ALWAYS parameterized: `log.info("Tenant {} bootstrapped by {}", id, userId)`.
- NEVER log secrets, tokens, full JWTs, or PII (compliance: Canada + Cameroon).

### Permissions
- Every protected endpoint enforces `@RequiresPermission(resource:action:scope)` from
  insurance-security-core. Unauthorized = 403; unauthenticated = 401.
- Seeded matrix already governs defaults (with the 4 approved corrections from M1).

### Testing
- Unit tests for every use case (mock the out-ports — no Spring, no DB).
- Integration tests (Testcontainers) for every endpoint and the Keycloak flows.
- @DataJpaTest / @SpringBootTest slices must @Import(JpaAuditingConfig.class) (G-007).
- Coverage >= 80% per module.

---

# MILESTONE 2 — Internal User Lifecycle + Approval Workflow

**Goal:** A new tenant can be bootstrapped, internal staff invited, approved by a branch manager,
and activated in Keycloak. Real Keycloak user creation lands here.

---

## US-M2-001 — Keycloak admin port + adapter (real integration)
**As a** platform, **I want** a Keycloak out-port with a real adapter, **so that** identity
operations create and manage actual users in the correct realm.

**Acceptance Criteria:**
- Out-port `KeycloakUserPort` in `domain/port/out/` (pure interface, domain terms):
  `createUser(...)`, `assignRealmRole(...)`, `sendPasswordResetEmail(...)`, `disableUser(...)`,
  `deleteUser(...)`, returning a Keycloak user id (String).
- Adapter `KeycloakUserAdapter` in `infrastructure/keycloak/` implements it using the Keycloak
  Admin client, targeting the INTERNAL realm (`lz-insurance-internal`).
- Realm/client/credentials come from config (never hardcoded); a `KeycloakProperties`
  (@ConfigurationProperties) binds them.
- Integration test (Testcontainers Keycloak) proving: create user -> user exists in realm ->
  assign role -> disable -> delete.
- On Keycloak failure, the adapter throws a domain-meaningful exception (not a raw Keycloak
  exception leaking into the domain).

## US-M2-002 — Tenant bootstrap ceremony
**As a** deploying operator, **I want** `POST /internal/bootstrap`, **so that** a brand-new tenant
gets its first TENANT_ADMIN created exactly once.

**Acceptance Criteria:**
- Endpoint protected by `X-Bootstrap-Secret` header (env var), NOT JWT.
- In-port `BootstrapTenant` use case (framework-free): validates the tenant is not already
  bootstrapped (idempotency guard — reject if `tenant.bootstrapped == true`), creates the
  TENANT_ADMIN IdentityProfile, creates the Keycloak user with a temporary password +
  required-action "UPDATE_PASSWORD", assigns the TENANT_ADMIN realm role, marks the tenant
  `bootstrapped = true`, emits a welcome email.
- Endpoint can never succeed twice for the same tenant (test it returns 409 on second call).
- Integration test: full bootstrap -> Keycloak user exists -> tenant flagged -> second call rejected.
- Wrong/missing bootstrap secret -> 403.

## US-M2-003 — Admin invites internal staff
**As a** TENANT_ADMIN or BRANCH_MANAGER, **I want** `POST /api/v1/users`, **so that** I can invite
staff who then go through approval before activation.

**Acceptance Criteria:**
- In-port `InviteInternalUser`: creates an IdentityProfile in `PENDING_APPROVAL` (NOT yet in
  Keycloak), creates an `ApprovalRequest` in `PENDING`, records requester.
- NO Keycloak user is created at this step (creation is deferred to approval).
- Requires `user:create` permission at the caller's scope; branch managers may only invite within
  their branch (BRANCH scope enforced).
- Duplicate email within tenant -> 409.
- Unit test (mocked ports) + integration test (endpoint + persistence).

## US-M2-004 — Branch manager approves / rejects
**As a** BRANCH_MANAGER, **I want** `POST /api/v1/approvals/{id}/approve` and `.../reject`,
**so that** I control who becomes an active user in my branch.

**Acceptance Criteria:**
- `ApproveRegistration` use case: only from `PENDING`; on approve -> create the Keycloak user
  (temp password + UPDATE_PASSWORD), assign role, transition profile PENDING_APPROVAL -> ACTIVE,
  set ApprovalRequest -> APPROVED with reviewer + timestamp, send activation email.
- `RejectRegistration`: PENDING -> REJECTED with reviewer + notes; NO Keycloak user created;
  profile transitions to a terminal rejected/deactivated state.
- Approving/rejecting an already-decided request -> 409 (ApprovalAlreadyDecidedException).
- Requires `user:approve:BRANCH`; a manager cannot approve outside their branch (403).
- Keycloak user is created ONLY on approve, NEVER before (explicit test).
- Integration test: invite -> approve -> Keycloak user exists + profile ACTIVE; invite -> reject
  -> no Keycloak user + profile rejected.

## US-M2-005 — Role assignment
**As a** TENANT_ADMIN/BRANCH_MANAGER, **I want** `POST /api/v1/users/{id}/roles` and
`DELETE /api/v1/users/{id}/roles/{roleId}`, **so that** I manage what staff can do.

**Acceptance Criteria:**
- `AssignRole` / `RemoveRole` use cases: create/remove `ProfileRoleAssignment`, sync the
  corresponding realm role in Keycloak.
- Cannot assign a role outside the caller's scope; cannot escalate beyond own privileges.
- Requires the appropriate permission (decide: dedicated `role:assign` vs `user:update` — see
  G-002; resolve the gap here).
- Tests: assign -> realm role present + assignment row; remove -> both gone; scope violation -> 403.

## US-M2-006 — Branch assignment
**As a** TENANT_ADMIN, **I want** `PUT /api/v1/users/{id}/branch`, **so that** I can move staff
between branches.

**Acceptance Criteria:**
- `AssignBranch` use case updates `IdentityProfile.branchId` (with valid-transition checks).
- Requires `user:update` at ALL or BRANCH as appropriate.
- Test: reassign branch -> persisted; branch-scoped data visibility follows the new branch.

## US-M2-007 — Profile read / update / deactivate
**As a** privileged user, **I want** `GET/PUT/DELETE /api/v1/users/{id}`, **so that** I can view,
edit, and deactivate staff.

**Acceptance Criteria:**
- GET returns the profile (scope-filtered: OWN/BRANCH/ALL via specification).
- PUT updates mutable fields (name, contact) — NOT actorType/userType (immutable invariants).
- DELETE = soft delete + Keycloak user disabled (NOT hard-deleted); profile -> DEACTIVATED.
- Each requires the matching `user:read`/`user:update`/`user:delete` permission at scope.
- Tests for each, including a 403 for insufficient scope.

## M2 Exit Gate
- Bootstrap end-to-end tested (incl. idempotency rejection).
- Full invite -> approve -> Keycloak activation flow tested; invite -> reject path tested.
- Keycloak user created ONLY on approval (verified).
- Every endpoint has an integration test; @RequiresPermission enforced (403/401 paths tested).
- Domain use cases unit-tested with mocked ports (NO Spring in those tests) — proving the
  framework-free design holds.
- Coverage >= 80%. `mvn clean install` green. G-002 (role-assignment permission) resolved.

---

# MILESTONE 3 — Authentication Flows

**Goal:** Tokens issued and validated, sessions tracked in Redis + session_registry, refresh and
revocation working, password lifecycle complete.

---

## US-M3-001 — Login + custom-claim token
**As a** staff/customer, **I want** to authenticate and receive a JWT enriched with identity
claims, **so that** downstream services know who I am and what I can do.

**Acceptance Criteria:**
- Login delegates to Keycloak (correct realm by actorType); on success the token carries custom
  claims: tenantId, branchId, actorType, internalUserType/externalUserType, roles, resolved
  permissions, sessionId.
- Permission resolution = system defaults merged with tenant overrides (RolePermissionConfig),
  cached in Redis.
- Test: login -> token contains correct claims for an INTERNAL and an EXTERNAL user.

## US-M3-002 — Token validation middleware
**As a** platform, **I want** every request validated against the issuing realm, **so that** only
valid tokens reach business logic.

**Acceptance Criteria:**
- Validation wired from insurance-security-keycloak; TenantContext populated from claims before any
  service method runs.
- Invalid/expired token -> 401; valid token with insufficient scope -> 403.
- Test both paths.

## US-M3-003 — Session creation + registry
**As a** platform, **I want** a session recorded on login, **so that** sessions are auditable and
revocable.

**Acceptance Criteria:**
- On login, create a SessionRegistry row (status ACTIVE, expiresAt, ip, userAgent) AND the
  authoritative Redis session entry.
- Redis is the source of truth; session_registry is the audit/admin view.
- Test: login creates both; isValid() true while active and unexpired.

## US-M3-004 — Token refresh
**Acceptance Criteria:**
- Refresh endpoint exchanges a valid refresh token for a new access token, updates
  lastActivityAt, picks up any permission changes within one TTL cycle.
- Expired/revoked refresh -> 401. Test the happy path + rejection.

## US-M3-005 — Logout + revocation
**Acceptance Criteria:**
- Logout transitions SessionRegistry ACTIVE -> REVOKED and removes the Redis entry within one
  request cycle.
- A revoked session's token is rejected on the next request (test it).

## US-M3-006 — Admin session management
**Acceptance Criteria:**
- `GET /api/v1/sessions` (active sessions for a user, scope-filtered) and
  `DELETE /api/v1/sessions/{id}` (admin revoke).
- Requires `session:read` / `session:delete` at scope.
- Test: admin revokes another user's session -> that session is rejected next request.

## US-M3-007 — Password lifecycle
**Acceptance Criteria:**
- `PUT /api/v1/users/{id}/password` (change, self or admin per scope).
- External self-service password reset via email (EXTERNAL realm).
- First-login forced password change honored (UPDATE_PASSWORD required-action from bootstrap/invite).
- Tests for change, reset, and forced-change-on-first-login.

## M3 Exit Gate
- Full login -> session (Redis + registry) -> refresh -> logout cycle tested.
- Revocation propagates to Redis within one request cycle (verified).
- 401 (invalid token) and 403 (insufficient scope) paths tested.
- Permission changes propagate within one token TTL (tested).
- Coverage >= 80%. `mvn clean install` green.

---

## Cross-cutting notes carried from M1
- G-003 (package casing) should be resolved by M1-009; M2/M3 code uses the normalized convention.
- G-009 (component-scan/auditing) resolved in M1-009 — auditing active in the running app.
- G-010 (tenant isolation opt-in) — M2/M3 enforce scope explicitly via specifications until
  insurance-security-multitenancy provides automatic injection.
