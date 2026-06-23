# LZ Insurance — Delivery Roadmap

> Load in Claude Code with: `@docs/roadmap.md`
> Do not store execution checklists here — use your issue tracker for that.
> This file describes WHAT each milestone delivers and its exit gate.

---

## Guiding Principles
- No milestone is "done" until its exit gate fully passes
- Architecture validation before code — always
- Every feature has unit + integration tests before it is considered complete
- Code coverage ≥ 80% enforced per service

---

## Phase 0 — Platform Bootstrap (Pre-M1)
**Status: COMPLETE**
- Maven multi-module project structure
- Shared infrastructure modules scaffolded
- Docker Compose local stack (Postgres, Redis, Keycloak, Kafka, Mailhog)
- CI pipeline (GitHub Actions: build + test on every PR)
- Base coding standards, JaCoCo, Checkstyle configured

---

## insurance-identity-service Milestones

---

### M1 — Foundation & Domain Layer
**Goal:** Working application skeleton, schema, and domain model. Everything boots. Data persists.

**Scope:**
- Domain model classes: `Tenant`, `Branch`, `IdentityProfile`, `SystemRole`, `Permission`, `RolePermission`, `RolePermissionConfig`, `ProfileRoleAssignment`, `ApprovalRequest`, `SessionRegistry`
- JPA entities (infrastructure layer)
- Spring Data repositories with Specification support
- Domain port interfaces (in-ports: use case interfaces; out-ports: repository interfaces)
- Application boots against docker-compose stack

**Exit Gate:**
- [ ] `mvn clean install` passes — zero errors
- [ ] All 15 Liquibase changelogs apply cleanly on fresh Postgres
- [ ] Seed data (roles, permissions, matrix) is present and queryable
- [ ] Repository integration tests pass (Testcontainers)
- [ ] Domain model unit tests pass
- [ ] Code coverage ≥ 80% on domain module

---

### M2 — Internal User Lifecycle + Approval Workflow
**Goal:** Internal staff can be invited, approved, and activated. Branch managers run the approval flow.

**Scope:**
- Bootstrap endpoint: `POST /internal/bootstrap` (first TENANT_ADMIN creation)
- Admin invite flow: `POST /api/v1/users` → creates `IdentityProfile` + `ApprovalRequest`
- Branch manager approval: `POST /api/v1/approvals/{id}/approve` | `.../reject`
- Keycloak account creation triggered ONLY after approval
- Role assignment: `POST /api/v1/users/{id}/roles`
- Branch assignment: `PUT /api/v1/users/{id}/branch`
- Profile read: `GET /api/v1/users/{id}`
- Profile update: `PUT /api/v1/users/{id}`
- Soft delete / deactivate: `DELETE /api/v1/users/{id}`

**Exit Gate:**
- [ ] Bootstrap flow end-to-end tested (integration test)
- [ ] Full invite → approval → Keycloak activation flow tested
- [ ] All REST endpoints covered by integration tests
- [ ] @RequiresPermission enforced on all endpoints (test unauthorized access returns 403)
- [ ] Code coverage ≥ 80%

---

### M3 — Authentication Flows
**Goal:** Tokens issued, validated, sessions tracked, refresh and revocation working.

**Scope:**
- Login flow (delegate to Keycloak, enrich JWT with custom claims)
- Token validation middleware wired from `insurance-security-keycloak`
- Session creation on login (Redis + session_registry)
- Token refresh
- Logout (session revocation in Redis + session_registry)
- Admin session query: `GET /api/v1/sessions` (active sessions for a user)
- Admin session revoke: `DELETE /api/v1/sessions/{id}`
- Password change: `PUT /api/v1/users/{id}/password`
- Password reset flow (external realm)

**Exit Gate:**
- [ ] Full login → session → refresh → logout cycle tested
- [ ] Session revocation propagates to Redis within one request cycle
- [ ] Unauthorized token returns 401; insufficient scope returns 403
- [ ] Code coverage ≥ 80%

---

### M4 — External Customer Self-Service
**Goal:** Policyholders can register, verify, log in, and manage their profile.

**Scope:**
- Self-registration: `POST /api/v1/customers/register`
- Email verification flow
- External login (Keycloak external realm)
- Password reset (self-service)
- Profile read/update for external customers
- Link between `IdentityProfile` and `CustomerService` (`identityProfileId` handshake)

**Exit Gate:**
- [ ] Full registration → verification → login cycle tested
- [ ] External actor cannot access internal endpoints (403)
- [ ] Code coverage ≥ 80%

---

### M5 — Permission Configuration & Tenant Admin UI API
**Goal:** Tenant admins can customize the permission matrix for their tenant. Changes propagate live.

**Scope:**
- `GET /api/v1/roles` — list roles with resolved permissions for tenant
- `GET /api/v1/roles/{id}/permissions` — view permission matrix for a role
- `PUT /api/v1/roles/{id}/permissions` — update tenant-level permission overrides
- Redis cache invalidation on permission change
- Permission resolution logic: system defaults merged with tenant overrides
- `RolePermissionConfig` CRUD

**Exit Gate:**
- [ ] Permission override written → Redis cache invalidated → next request sees new permissions (tested)
- [ ] System-defined roles cannot have their `roleType` changed (test for 400)
- [ ] Code coverage ≥ 80%

---

### M6 — Audit, Session Admin & Security Hardening
**Goal:** Full audit trail, admin tooling for session management, production security posture.

**Scope:**
- All identity events emitted to `insurance-audit-service` via messaging
- Admin dashboard API: active sessions, login history, failed login attempts
- Account lockout after N failed attempts (configurable per tenant)
- MFA enforcement for TENANT_ADMIN and BRANCH_MANAGER
- Rate limiting on auth endpoints
- Penetration test checklist pass

**Exit Gate:**
- [ ] All domain events emitted and consumed by audit service (integration test)
- [ ] Account lockout tested
- [ ] MFA flow tested for admin roles
- [ ] OWASP Top 10 checklist reviewed
- [ ] Code coverage ≥ 80%

---

## Subsequent Services (post identity-service)
Order of implementation after identity-service is complete:

1. `insurance-customer-service` — M1: profile, KYC data, identity link
2. `insurance-policy-service` — M1: quoting engine (Canadian personal auto first)
3. `insurance-telematics-service` — M1: trip ingestion, scoring engine (UBI feature)
4. `insurance-ubi-pricing-service` — M1: score → discount calculation
5. `insurance-claims-service` — M1: FNOL, workflow
6. `insurance-billing-service` — M1: premium invoice, payment
7. `insurance-notification-service` — M1: email/SMS templates, dispatch
8. `insurance-document-service` — M1: policy document generation
9. `insurance-mobile-money-service` — M1: MTN MoMo + Orange Money (Cameroon)
10. `insurance-regulatory-service` — M1: Canadian regulatory reporting
