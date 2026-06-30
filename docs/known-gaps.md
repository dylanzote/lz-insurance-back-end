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

### G-011 — "SYSTEM" auditor fallback is unreachable under a real security context
**What:** `JpaAuditingConfig.auditorProvider` falls back to `"SYSTEM"` when `getCurrentUserId()`
returns `null`. But `CurrentUserService.getCurrentUserId()` delegates to `AuthUser.anonymous()`,
whose `userId` is the literal `"anonymous"` (never `null`). So once the security stack is wired
(M3), an unauthenticated request would be audited as `"anonymous"`, and the `"SYSTEM"` Optional
branch is dead code. (In M1 the fallback IS reached, because no `CurrentUserService` bean exists —
the `ObjectProvider` resolves empty and the config returns `"SYSTEM"` directly.)
**Impact:** Cosmetic in M1. In M3 system/bootstrap actions could be mis-attributed to `"anonymous"`
instead of `"SYSTEM"`, and unauthenticated mutations would not be visibly distinguished.
**Resolve by:** M3, when the security stack + `CurrentUserService` are wired. Decide the intended
auditor for no-auth/bootstrap paths (likely make `getCurrentUserId()` return `null` when anonymous,
or special-case bootstrap) and add a test that a real username — not `"SYSTEM"`/`"anonymous"` —
lands in `created_by`.
**Flagged:** during US-M1-009 boot verification.

### G-012 — JaCoCo 0.8.13 can't instrument Java 26 classes (Maven runs on JDK 26)
**What:** `java` on PATH is Temurin 25.0.3, but Maven runs on Homebrew OpenJDK 26.0.1, so the
forked test JVM is Java 26 (class-file major 70). JaCoCo 0.8.13 supports up to Java 25 (major 69),
so it logs `IllegalClassFormatException: Unsupported class file major version 70` when the boot IT's
JDK `HttpClient` triggers instrumentation of `jdk.internal.net.http.*` runtime classes.
**Impact:** Cosmetic only. It is JDK-internal classes that fail — never project code. Our modules
compile to target 25 (major 69) and instrument fine; domain coverage measured cleanly at 97.8%.
The build is green. Just noisy logs in `insurance-identity-infrastructure`.
**Resolve by:** when JaCoCo ships Java 26 support (bump `jacoco-plugin.version`), or pin Maven to
run on JDK 25 (`JAVA_HOME`), or exclude the boot IT's JDK-internal instrumentation. Low priority.
**Flagged:** during US-M1-009 JaCoCo wiring.

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

### G-003 — Package casing split (com.lz_Insurance vs com.lz_insurance) — RESOLVED 2026-06-30 (G-003 sweep)
**What:** The codebase had two package roots differing only by case — the dominant capital-`I`
`com.lz_Insurance` plus the lowercase `com.lz_insurance` used by `insurance-identity-infrastructure`.
Two packages differing only by case is fragile on case-insensitive filesystems (macOS default).
**Resolution:** A dedicated, isolated mechanical sweep (branch `feature/g003-package-casing-sweep`)
normalized everything to all-lowercase `com.lz_insurance` — the proper Java convention. This is now
the ONLY convention; capital-`I` `com.lz_Insurance` no longer exists anywhere in the codebase.
**Scope executed (verified):**
- **191 `.java` files** content-normalized (135 `package` decls + 115 with capital-`I` imports;
  406 total refs). Included the already-lowercase infra module's 56 cross-module imports of
  core/persistence/domain, which had to flip in lockstep.
- **17 source-root directories** `com/lz_Insurance` → `com/lz_insurance` (the 2 infra source roots
  were already lowercase and left untouched). On the case-insensitive FS the `git mv` two-step dance
  reverted 10 files' working-tree content to the committed capital-`I` blob; recovered by re-running
  the content replace against the physical tree (`find -exec sed`, since BSD `grep -r` was caching
  the renamed-by-case dirs). Lesson: verify physical content after case-only renames, not via
  `git grep`/`grep -r`.
- **20 `pom.xml`** (68 refs): parent declarations, own `groupId`s, and dependency coordinates —
  parent↔child consistency verified (incl. infra's deps on `…identity.api`/`…domain`/`persistence`).
- Stale `capital-I` rationale scrubbed from `InsuranceIdentityInfrastructureApplication`'s Javadoc.
**Wiring (re-evaluated, deliberately unchanged):** the targeted `@Import(JpaAuditingConfig.class)`
on the application class was NEVER a casing workaround — `com.lz_insurance.persistence` sits outside
the `com.lz_insurance.insurance` component-scan root, and broadening the scan would drag in the
unconfigured Keycloak/security stack (see G-009). It stays targeted; **M3 (real security wiring) is
the natural point to revisit `@Import` vs. broadened scan**, not now.
**Verification:** `mvn clean install` (Docker up) green — all 20 modules build, 49 domain tests +
37 Failsafe ITs (35 adapters + 2 boot) pass, JaCoCo "All coverage checks have been met". Zero
capital-`I` `lz_Insurance` remains anywhere (incl. `target/`). The G-012 JaCoCo/Java-26 log noise
is unrelated and still cosmetic.
**Flagged:** during US-M1-007 persistence inventory.

### G-009 — Identity app component-scan misses the capital-I shared beans — RESOLVED 2026-06-29 (US-M1-009)
**What:** `InsuranceIdentityInfrastructureApplication` scanned only lowercase
`com.lz_insurance.insurance`, so the capital-I `JpaAuditingConfig`/`CurrentUserService` (and other
shared beans, which also sit *outside* the `.insurance.` segment) were never loaded → JPA auditing
inactive → `NOT NULL` `created_by`/`created_at` would fail on first insert.
**Resolution:** Targeted wiring, NOT scan-broadening (broadening would have dragged in the whole
unconfigured Keycloak/security stack — wrong for M1). `JpaAuditingConfig` is now pulled via an
explicit `@Import` on the application class. Its `auditorProvider` was changed to take
`ObjectProvider<CurrentUserService>` so auditing activates without a `CurrentUserService` bean,
falling back to `"SYSTEM"` (see G-011). Spring Security is on the classpath transitively but unused
in M1, so the servlet-security auto-configs (`SecurityAutoConfiguration`,
`ServletWebSecurityAutoConfiguration`, `ManagementWebSecurityAutoConfiguration`,
`UserDetailsServiceAutoConfiguration`) are excluded so `/actuator/health` stays open; Redis arrives
transitively too, so `DataRedisAutoConfiguration` is excluded (no Redis until M3). Verified by
`IdentityApplicationBootIT`: full context boots, all 13 changelogs apply, an insert succeeds with
`created_by="SYSTEM"`, and `/actuator/health` returns UP without auth. Decoupled from G-003 (the
casing sweep is no longer load-bearing).
**Flagged:** during US-M1-008 Tenant repository slice.

### Defects A & B — Repository ITs never executed / fixture collided with seed — RESOLVED 2026-06-29 (US-M1-009)
**A — ITs not run by the build.** Maven Surefire's default patterns are `*Test`/`*Tests`; the
repository ITs are named `*IT`, and there was no Failsafe plugin and no Surefire `<includes>`. So
`mvn clean install` silently skipped all 10 persistence ITs — the "repository ITs pass" exit-gate
item had never actually been exercised by the build (the prior memory claim that Surefire ran them
was wrong). **Fix:** added `maven-failsafe-plugin` to the ROOT pom (`integration-test` + `verify`
goals, default `*IT` convention), inherited by every module. Because `verify` precedes `install`,
`mvn clean install` now runs ITs automatically (Testcontainers → requires Docker).
**B — `persistPermission()` collided with seeded data.** The slice runs the full Liquibase master
incl. seed 011–013, which inserts `perm-policy-read = POLICY/READ`. The shared fixture hardcoded a
`POLICY/READ` insert → `duplicate key uq_permissions_resource_action` on every test that seeded a
Permission (9 of 35 errored once they actually ran). **Fix:** FK-only callers now use
`seededPermissionId()` (fetches a committed seed row) instead of inserting; the three
creation-specific `PermissionRepositoryAdapterIT` tests use confirmed-unseeded resource/action
combos, each guarded by a setup assertion that fails loudly if a future seed adds that combo.
**Result:** full `mvn clean install` green — 49 domain + 1 infra context + 37 Failsafe IT methods
(35 across 10 adapters + 2 boot).
**Flagged:** during US-M1-009 boot verification.

### JaCoCo coverage gate — WIRED & MEASURED 2026-06-29 (US-M1-009 / M1 close)
**What:** `coverage >= 80%, enforced via JaCoCo` was referenced everywhere (root CLAUDE.md,
m1-user-stories, exit gate) but the plugin did not exist in any pom — the gate had never been
measured.
**Resolution:** Added `jacoco-maven-plugin` to the ROOT pom (pluginManagement + active:
`prepare-agent` + `report` on `verify`), inherited by all modules — `mvn clean install` now emits
`target/site/jacoco`. A check rule on `insurance-identity-domain` enforces **>= 80% LINE coverage**
(`element=BUNDLE`, `haltOnFailure=true`). Measured actual: **97.8% line** (354/362), 98.3%
instruction, 91.7% branch, 96.0% method — well clear of the floor, so enforcement was turned on
build-failing immediately. Lowest classes are two tiny exception types and `DomainGuard`; not padded
with trivial tests (per policy). See G-012 for a JDK-26/JaCoCo log-noise caveat.
**Flagged:** during M1 exit-gate review.

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
