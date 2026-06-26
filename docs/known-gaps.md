# LZ Insurance — Known Gaps & Deferred Items

> Load in Claude Code with: `@docs/known-gaps.md`
> This file exists so deferred decisions and known issues are NOT lost between sessions.
> Every entry: what it is, why it's deferred, and when it must be resolved.
> When an item is resolved, move it to the "Resolved" section at the bottom with the date.

---

## Open — must be resolved before the milestone noted

### G-001 — SystemRole has no tenantId
**What:** The `SystemRole` domain model has no `tenantId` field, so CUSTOM (tenant-created)
roles cannot be tenant-scoped — every role is effectively a global platform default.
**Impact:** Tenants cannot create their own roles. The RBAC model only supports the fixed
SYSTEM roles plus per-tenant *permission overrides* (via `RolePermissionConfig`), not custom roles.
**Resolve by:** M5 (Permission Configuration & Tenant Admin API). Decide then whether CUSTOM roles
are in scope; if so, add `tenantId` to `SystemRole` (or split into `SystemRole` + `TenantRole`).
**Flagged:** during US-M1-007 persistence inventory.

### G-002 — No explicit permission gates role assignment
**What:** There is no `role:assign` (or equivalent) permission. The authority to attach a role
to an `IdentityProfile` is currently implicit, riding under `user:update`.
**Impact:** Role-assignment authority can't be granted or restricted independently of general
user editing — a privilege-escalation risk if `user:update` is granted broadly.
**Resolve by:** M2 (Internal User Lifecycle) when role assignment endpoints are built. Decide
whether to introduce a dedicated `permission`/`role` action for assignment.
**Flagged:** during role->permission matrix review.

### G-003 — Package casing split (com.lz_Insurance vs com.lz_insurance)
**What:** The dominant package convention is `com.lz_Insurance` (capital `I`). One infrastructure
stub uses lowercase `com.lz_insurance`. Two packages differing only by case is fragile, especially
on case-insensitive filesystems (default on macOS).
**Correct target:** all-lowercase `com.lz_insurance` is the proper Java convention.
**Impact:** Potential build/classpath ambiguity; inconsistent codebase.
**Resolve by:** a dedicated cleanup task BETWEEN milestones (not mid-milestone). Normalize ALL
packages to lowercase `com.lz_insurance` in one sweep, with a single refactor commit.
**Interim rule:** match the dominant `com.lz_Insurance` for all new code until the sweep.
**Flagged:** during US-M1-007 persistence inventory.

### G-004 — AuditLogListener NPE on null version
**What:** `AuditLogListener` (insurance-persistence) duplicates `BaseDomainEntity`'s lifecycle
callbacks, and its `preUpdate` does `getVersion() + 1`, which throws NPE when version is null.
**Interim handling:** the listener is NOT attached; entities rely on `BaseDomainEntity`'s own
`@PrePersist`/`@PreUpdate`. A `// FIXME` comment marks the NPE in insurance-core.
**Impact:** Latent bug in a shared module every service inherits.
**Resolve by:** a shared-infrastructure cleanup pass. Either fix the null-guard in the listener
and remove the duplication from `BaseDomainEntity`, or delete the listener entirely if redundant.
**Flagged:** during US-M1-007 persistence inventory.

### G-005 — POLICYHOLDER policy:create and policy/claim/report grants are provisional
**What:** Seed permission grants for `policy:*`, `claim:*`, `report:*` target services that do
not exist yet (policy-service, claims-service). In particular, POLICYHOLDER `policy:create`
models "apply for a policy" but Canadian insurance binds policies via agent/underwriter, not
the customer directly.
**Impact:** These default grants are best-guesses and may not match the real service designs.
**Resolve by:** the design of each owning service (policy-service M1, claims-service M1). Revisit
and correct the matrix then. The identity-owned grants (user/branch/tenant/permission/session/audit)
are considered FINAL for M1.
**Flagged:** during role->permission matrix review.

---

## Resolved

### G-006 — No identity Postgres in docker-compose — RESOLVED 2026-06-23 (US-M1-007)
**What:** `docker-compose.yml` defined Postgres instances for customer/product/policy
(5432/5433/5434) but none for identity, and `application.properties` had no datasource/Liquibase
config — the identity app could not boot or run integration tests.
**Resolution:** Added `postgres-identity` (image `postgres:16-alpine`, DB `identity_db`, port
`5435:5432`, volume `postgres_identity_data`) to `docker-compose.yml`, plus datasource +
`spring.liquibase` config (master changelog) in
`insurance-identity-infrastructure/src/main/resources/application.properties`.
**Flagged:** during US-M1-007 persistence inventory.
