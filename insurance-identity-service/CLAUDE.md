# insurance-identity-service — Service Context

> Loaded when Claude works in the `insurance-identity-service` directory.
> Root `CLAUDE.md` is always loaded first — this file adds service-specific depth.
> **This file reflects the ACTUAL state of the repo. Do not trust any status claim that
> isn't verifiable in the code — inventory first, then act.**

---

## Service Responsibility
Single deployable Spring Boot service. Owns ALL identity concerns:
- Authentication delegation (Keycloak)
- Identity profile management (internal staff + external customers)
- Role & permission assignment
- Branch-scoped access control
- Session lifecycle (Redis + Postgres)
- Tenant bootstrap ceremony

Does NOT own: policy data, claims data, KYC business data, customer financials.

---

## Conventions (as they ACTUALLY are in the repo)
- **Package root:** `com.lz_Insurance.insurance...` (capital `I` in `lz_Insurance`).
  This is the dominant convention across domain, core, persistence, and api.
  WARNING: One infra stub uses lowercase `com.lz_insurance` — match the capital-`I` form for all
  new code. "Normalize all packages to lowercase" is a tracked cleanup (see `docs/known-gaps.md`),
  NOT to be done mid-milestone.
- **Base entity:** `BaseDomainEntity` lives in **insurance-core** (`com.lz_Insurance.core.model`),
  NOT in insurance-persistence. It is a plain domain base — NOT JPA-mapped.
- **JPA base (decided):** a separate `BaseJpaEntity` (`@MappedSuperclass`) lives in
  **insurance-persistence**, mirroring the audit/id/version field-set. JPA entities extend it.
  The two base classes intentionally duplicate the field-set across the architectural boundary —
  each carries a cross-reference comment so nobody "fixes" the duplication by merging them.

---

## Maven Modules (hexagonal)
```
insurance-identity-service/
  insurance-identity-api/            # Incoming ports: controllers, DTOs, OpenAPI
    ...controller/         # REST controllers (incoming adapters) iterfaces with api documentation
    ...usecase/            # Use case implementations service of the controllers
    ...dto/                # Request/Response DTOs
    ...mapper/             # DTO ↔ Domain mappers
    ...openapi/            # OpenAPI config
  insurance-identity-domain/         # Pure business logic — ZERO framework imports
    .../domain/model/                # 10 domain models (extend core BaseDomainEntity)
    .../domain/enumeration/          # 13 enums
    .../domain/exception/            # 3 domain exceptions
    .../domain/support/              # DomainGuard
    .../domain/port/in/              # use case interfaces (next task) called by api 
    .../domain/port/out/             # repository/external interfaces (next task)
  insurance-identity-infrastructure/ # Adapters: JPA entities, repos, Keycloak, Redis, config
    .../infrastructure/persistence/entity/        # JPA entities
    .../infrastructure/persistence/repository/    # Spring Data repos
    .../infrastructure/persistence/adapter/       # implements domain out-ports + MapStruct mappers
    .../infrastructure/persistence/specification/ # JPA Specifications
    src/main/resources/db/changelog/              # Liquibase (master + 001-010 tables, 011-013 seed)
```

---

## Domain Layer — STATUS: COMPLETE
- **10 domain models** — all extend core `BaseDomainEntity`, validate in constructor,
  expose state via getters with NO public setters (invariants can't be bypassed).
  State machines on `IdentityProfile`, `ApprovalRequest`, `SessionRegistry`;
  HQ-no-parent rule on `Branch`; actorType <-> userType rule on `IdentityProfile`.
- **`reconstitute(...)` factory on all 10 models** — PUBLIC static; rebuilds already-valid
  stored state (id, version, current status, keycloakUserId, historical timestamps) with
  STRUCTURAL validation only (no creation-time state rules re-applied). Constructors unchanged.
- **13 enums** — ActorType, IdentityStatus, InternalUserType, ExternalUserType,
  OrganizationScope, ApprovalStatus, SessionStatus, PermissionAction, PermissionResource,
  RoleType, TenantStatus, BranchType, BranchStatus.
- **3 exceptions** — DomainValidationException, InvalidStateTransitionException,
  ApprovalAlreadyDecidedException (layered on core exception hierarchy).
- **DomainGuard** — guard clauses throwing the domain exception type.
- **49 unit tests passing** (41 original + 8 reconstitution round-trip), zero framework imports.

DO NOT modify the domain module during persistence work unless a genuinely new capability
is required (as reconstitution was) — and flag it before doing so.

---

## Domain Models
| Class | Notes |
|---|---|
| `Tenant` | name, code(unique), country(ISO-2), bootstrapped, headquarterBranchId, status |
| `Branch` | tenantId, parentBranchId(null=top), name, code, type, status. HQ has no parent. |
| `IdentityProfile` | actorType, internal/externalUserType (mutually exclusive), keycloakUserId, status. State machine. |
| `SystemRole` | name, roleType, description. WARNING: NO tenantId (see known-gaps). |
| `Permission` | resource, action, defaultScope |
| `RolePermission` | roleId, permissionId, grantedScope — system defaults |
| `RolePermissionConfig` | tenantId, roleId, permissionId, grantedScope — tenant overrides |
| `ProfileRoleAssignment` | identityProfileId, roleId, tenantId, branchId(null=tenant-wide) |
| `ApprovalRequest` | identityProfileId, requestedById, reviewedById, status, notes. State machine. |
| `SessionRegistry` | identityProfileId, sessionToken, status, expiresAt, lastActivityAt. isValid(). |

---

## Persistence Layer — STATUS: IN PROGRESS (current task, US-M1-007)
**Liquibase changelogs DO NOT YET EXIST** — they are being authored now and become the schema
source of truth (no "drift to flag" — both sides defined together).

Task scope:
1. POM/config prerequisites — MapStruct + lombok-mapstruct-binding to root dependencyManagement;
   infra pom deps (insurance-persistence, insurance-identity-domain, MapStruct, Testcontainers);
   `BaseJpaEntity` in insurance-persistence; `postgres-identity` in docker-compose; datasource +
   Liquibase config in application.properties.
2. Liquibase changelogs — master + 001-010 (tables) + 011-013 (seed). YAML, snake_case, one
   changeset per file. **Pre-production rule: edit in place, no new changesets for corrections.**
3. JPA entities — one `{Model}Entity` per domain model, extend `BaseJpaEntity`, `@Enumerated(STRING)`,
   data containers ONLY (zero business logic), mapped exactly to the changelog tables.
4. MapStruct mappers — Domain->Entity via getters; Entity->Domain via `reconstitute(...)`.
   Hand-written mappers (calling `reconstitute`) are the fallback if MapStruct can't target the
   factory cleanly. NEVER BeanUtils (requires setters the domain deliberately lacks).

**Seed data:** deterministic IDs (`perm-policy-read-own`, `role-agent-...`). The role->permission
matrix is a business decision — seed the role/permission catalog, but the `role_permissions`
junction is seeded ONLY from the matrix the product owner has explicitly approved.

---

## STATUS — M1 COMPLETE (2026-06-29)
- **US-M1-008 DONE:** domain out-ports + Spring Data repos + adapters + specifications +
  Testcontainers ITs for all 10 aggregates.
- **US-M1-009 DONE:** app boots against a real Postgres, all 13 changelogs apply, seed present,
  JPA auditing active (`created_by` populated), `/actuator/health` UP. Wiring lives on
  `InsuranceIdentityInfrastructureApplication`: `@Import(JpaAuditingConfig)` (targeted, not a scan
  broaden — see G-009), servlet-security + `DataRedisAutoConfiguration` auto-configs excluded (no
  auth/Redis until M3), actuator health exposed. Verified by `IdentityApplicationBootIT`.

### Running integration tests (Failsafe — requires Docker)
The repository/boot ITs are `*IT` and run via `maven-failsafe-plugin` (declared in the ROOT pom,
inherited by all modules). Because `verify` precedes `install`, `mvn clean install` runs them
automatically — they are **Testcontainers-backed and need a running Docker daemon**. The default
`@SpringBootTest` `contextLoads` (Surefire) instead connects to the docker-compose `postgres-identity`
on `localhost:5435`, so bring that container up before a full build (`docker compose up -d
postgres-identity`).

## NEXT — M2 (Internal User Lifecycle + Approval Workflow)
- Domain in-ports (use-case interfaces) + API controllers/DTOs + use-case implementations.
- Immediate cleanup candidate: the G-003 lowercase package sweep (its own commit/PR).

---

## Known traps in shared modules (do not silently inherit)
- **Audit mechanism (settled 2026-06-29):** `BaseJpaEntity` generates `id` via `IdGenerator` in a
  `@PrePersist` (id only) and populates `createdBy`/`updatedBy`/timestamps through Spring Data JPA
  auditing (`@EntityListeners(AuditingEntityListener.class)` + `JpaAuditingConfig`'s `auditorProvider`,
  "SYSTEM" fallback). `@Version` handles optimistic locking. `BaseDomainEntity` is now framework-free
  (no JPA lifecycle). The old buggy `AuditLogListener` was DELETED (G-004). JPA auditing must be
  active wherever entities persist — `@DataJpaTest` slices `@Import(JpaAuditingConfig.class)` (G-007),
  and the running app loads it via an explicit `@Import` on the application class (G-009 RESOLVED in
  US-M1-009). `auditorProvider` now takes `ObjectProvider<CurrentUserService>`, so it activates
  without the security stack and falls back to `"SYSTEM"` when no `CurrentUserService` bean exists.
- See `docs/known-gaps.md` for the full deferred list (package casing G-003, SystemRole tenantId G-001,
  app bean-wiring G-009, opt-in tenant isolation G-010, etc.).

---

## M1 Exit Gate — ALL PASS (verified 2026-06-29)
1. [x] `mvn clean install` passes — zero errors (Docker daemon + compose `postgres-identity` up)
2. [x] All 13 changelogs apply cleanly on a fresh Postgres (Testcontainers, per IT class)
3. [x] Seed data present and queryable (31 permissions, 9 system roles, grant matrix)
4. [x] Domain + repository tests pass — 49 domain (Surefire) + 37 IT (Failsafe: 35 across 10
   adapters + 2 boot) + 1 infra contextLoads
5. [x] Coverage >= 80% on domain module — **97.8% line** (enforced, `haltOnFailure=true`); see
   G-012 for a JDK-26/JaCoCo log-noise caveat (cosmetic)
6. [x] App boots + `/actuator/health` UP — proven by `IdentityApplicationBootIT` (also asserts
   `created_by="SYSTEM"` on a real insert)

Deferred (do not block M1, tracked in `docs/known-gaps.md`): G-001, G-002, G-003 (lowercase sweep —
next task), G-005, G-008, G-010, G-011, G-012.

## Reference
- `@docs/identity-service/m1-user-stories.md` — M1 acceptance criteria
- `@docs/known-gaps.md` — deferred items that must not be lost
