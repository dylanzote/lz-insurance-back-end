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

## NOT YET STARTED
- Domain ports (in/out) + Spring Data repositories + adapters + Testcontainers IT (US-M1-008)
- Boot verification + M1 exit gate (US-M1-009): app boots against docker-compose,
  all changelogs apply, seed present, `/actuator/health` UP, coverage >= 80%

---

## Known traps in shared modules (do not silently inherit)
- **Audit mechanism (settled 2026-06-29):** `BaseJpaEntity` generates `id` via `IdGenerator` in a
  `@PrePersist` (id only) and populates `createdBy`/`updatedBy`/timestamps through Spring Data JPA
  auditing (`@EntityListeners(AuditingEntityListener.class)` + `JpaAuditingConfig`'s `auditorProvider`,
  "SYSTEM" fallback). `@Version` handles optimistic locking. `BaseDomainEntity` is now framework-free
  (no JPA lifecycle). The old buggy `AuditLogListener` was DELETED (G-004). JPA auditing must be
  active wherever entities persist — `@DataJpaTest` slices `@Import(JpaAuditingConfig.class)` (G-007),
  and the app itself does not yet load it (G-009).
- See `docs/known-gaps.md` for the full deferred list (package casing G-003, SystemRole tenantId G-001,
  app bean-wiring G-009, opt-in tenant isolation G-010, etc.).

---

## M1 Exit Gate (do not advance to M2 until ALL pass)
1. `mvn clean install` passes — zero errors
2. All changelogs apply cleanly on fresh Postgres (Testcontainers)
3. Seed data present and queryable
4. Domain + repository tests pass
5. Coverage >= 80% on domain module
6. App boots + `/actuator/health` UP

## Reference
- `@docs/identity-service/m1-user-stories.md` — M1 acceptance criteria
- `@docs/known-gaps.md` — deferred items that must not be lost
