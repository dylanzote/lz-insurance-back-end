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

### G-007 — JPA auditing must be active in @DataJpaTest slices
**What:** Audit fields are now populated by Spring Data JPA auditing (`@EnableJpaAuditing` in
`JpaAuditingConfig` + `AuditingEntityListener` on `BaseJpaEntity`), not by hand-rolled lifecycle
callbacks. `@DataJpaTest` slices do NOT pick up `@Configuration` beans by default, so auditing is
inactive in those slices.
**Impact:** In US-M1-008 (and any future `@DataJpaTest`), inserts fail because `created_at` and
`created_by` are `NOT NULL` and never get populated.
**Interim handling:** none yet — applies when persistence integration/slice tests are written.
**Resolve by:** US-M1-008. Every `@DataJpaTest` must `@Import(JpaAuditingConfig.class)` (and provide
a `CurrentUserService` bean / security context, or accept the `"SYSTEM"` fallback) so auditing runs.
**Flagged:** during the audit-mechanism cleanup (2026-06-29).

### G-008 — @JsonFormat / @NotNull live on the domain base, not the DTO layer
**What:** `BaseDomainEntity` (insurance-core) still carries `@JsonFormat` (Jackson serialization)
and `@NotNull` (Bean Validation) on its fields. These are presentation/validation concerns, not
domain-model concerns. The domain base is now runtime-framework-free (no `jakarta.persistence`),
but these compile-time annotations remain.
**Impact:** Minor leak of serialization/validation framework annotations into the domain base.
**Resolve by:** when the API/DTO layer is built — move `@JsonFormat`/`@NotNull` onto the
request/response DTOs and drop them from the domain base.
**Flagged:** during the audit-mechanism cleanup (2026-06-29).

### G-009 — Identity app component-scan misses the capital-I shared beans
**What:** `InsuranceIdentityInfrastructureApplication` declares
`@ComponentScan(basePackages = {"com.lz_insurance.insurance"})` (lowercase). Every shared module
is capital-I `com.lz_Insurance.*` — including `JpaAuditingConfig` and `CurrentUserService`. So
none of those beans are loaded by the running app: **JPA auditing is NOT active**, and after the
G-004 cleanup nothing else populates `created_by`/`created_at` → every insert will fail the
`NOT NULL` audit columns once the app boots against a real DB.
**Impact:** The app cannot persist anything until shared infrastructure beans are wired. Repository
*slice* tests are unaffected — they `@Import(JpaAuditingConfig.class)` explicitly (see G-007).
**Resolve by:** US-M1-009 (boot verification). Broaden the scan (add `com.lz_Insurance` roots) or
add explicit `@Import`s for the required shared configs, and ensure a `CurrentUserService` bean is
available. Verify `created_by` is populated by a real insert. Relates to G-003 (package casing).
**Flagged:** during US-M1-008 Tenant repository slice.

### G-010 — Tenant isolation is OPT-IN until insurance-security-multitenancy is wired
**What:** Tenant/branch predicates are applied only when a query explicitly uses
`TenantScopedSpecification` (`withTenantId`/`withBranchId`). `BaseSpecification` has no tenant
fields, and the automatic `tenantId`/`branchId` predicate injection described in the root
architecture (owned by `insurance-security-multitenancy`) is NOT wired.
**Impact:** SECURITY-RELEVANT. Until the multitenancy module is active, cross-tenant isolation is
NOT automatically enforced at the persistence layer — any repository call that forgets the scoped
specification returns rows across tenants. Safe for M1 (no live multi-tenant traffic), but must be
closed before any real tenant data coexists.
**Resolve by:** M2/M3 when `insurance-security-multitenancy` (TenantContext filter + automatic
predicate injection) is wired in. Until then, treat tenant scoping as a caller responsibility.
**Flagged:** during US-M1-008 Tenant repository slice.

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

### G-004 — AuditLogListener NPE on null version — RESOLVED 2026-06-29 (audit-mechanism cleanup)
**What:** `AuditLogListener` (insurance-persistence) duplicated `BaseDomainEntity`'s lifecycle
callbacks, and its `preUpdate` did `getVersion() + 1`, throwing NPE when version was null. It was
dead code (never attached via `@EntityListeners`) and superseded by JPA auditing.
**Resolution:** Deleted `AuditLogListener` entirely. Consolidated to ONE audit mechanism:
`BaseJpaEntity` keeps its `@PrePersist` for `id` generation (via `IdGenerator`) and uses Spring
Data JPA auditing (`@EntityListeners(AuditingEntityListener.class)` + `@CreatedBy`/`@LastModifiedBy`/
`@CreatedDate`/`@LastModifiedDate`) for the people/time fields, backed by `JpaAuditingConfig`'s
`auditorProvider` (real user from `CurrentUserService`, `"SYSTEM"` fallback). `BaseDomainEntity`
became runtime-framework-free (removed `jakarta.persistence` `@PrePersist`/`@PreUpdate`). Native
`@Version` optimistic locking retained — manual version increment dropped.
**Flagged:** during US-M1-007 persistence inventory.

### G-006 — No identity Postgres in docker-compose — RESOLVED 2026-06-23 (US-M1-007)
**What:** `docker-compose.yml` defined Postgres instances for customer/product/policy
(5432/5433/5434) but none for identity, and `application.properties` had no datasource/Liquibase
config — the identity app could not boot or run integration tests.
**Resolution:** Added `postgres-identity` (image `postgres:16-alpine`, DB `identity_db`, port
`5435:5432`, volume `postgres_identity_data`) to `docker-compose.yml`, plus datasource +
`spring.liquibase` config (master changelog) in
`insurance-identity-infrastructure/src/main/resources/application.properties`.
**Flagged:** during US-M1-007 persistence inventory.
