# M1 User Stories — insurance-identity-service

> Load in Claude Code with: `@docs/identity-service/m1-user-stories.md`

---

## US-M1-001 — Domain Model: Tenant

**As a** system architect,
**I want** a `Tenant` domain model class,
**So that** all identity operations are scoped to a tenant with full organizational context.

**Acceptance Criteria:**
- `Tenant` extends `BaseDomainEntity` (from `insurance-persistence`)
- Fields: `name` (String, required), `code` (String, unique, required), `country` (String, ISO 3166-1 alpha-2), `bootstrapped` (boolean, default false), `headquarterBranchId` (String), `status` (TenantStatus enum: ACTIVE, SUSPENDED, DEACTIVATED)
- `Tenant` is in the domain layer — zero framework imports
- All fields validated in constructor (null checks with domain exceptions, not javax.validation)
- Unit test: valid construction, invalid construction throws `DomainValidationException`

---

## US-M1-002 — Domain Model: Branch

**As a** system architect,
**I want** a `Branch` domain model with hierarchical support,
**So that** organizational structure (HQ → branches → sub-branches) is modelled correctly.

**Acceptance Criteria:**
- `Branch` extends `BaseDomainEntity`
- Fields: `tenantId` (String, required), `parentBranchId` (String, nullable — null = top-level), `name`, `code`, `type` (BranchType enum: HEADQUARTER, REGIONAL, LOCAL), `status` (BranchStatus: ACTIVE, INACTIVE)
- A `HEADQUARTER` branch cannot have a `parentBranchId` (domain rule enforced in model)
- Unit test: hierarchy rule enforced — creating HEADQUARTER with parentBranchId throws exception

---

## US-M1-003 — Domain Model: IdentityProfile

**As a** system architect,
**I want** a unified `IdentityProfile` domain model,
**So that** both internal staff and external customers share one identity model differentiated by `actorType`.

**Acceptance Criteria:**
- `IdentityProfile` extends `BaseDomainEntity`
- Fields: `tenantId`, `branchId` (nullable for external), `actorType` (ActorType enum), `internalUserType` (nullable — only for INTERNAL), `externalUserType` (nullable — only for EXTERNAL), `keycloakUserId` (String, nullable until activated), `email`, `firstName`, `lastName`, `status` (IdentityStatus enum)
- Domain rule: `internalUserType` must be null when `actorType = EXTERNAL` and vice versa
- Domain rule: `branchId` is required for INTERNAL profiles
- `activate(String keycloakUserId)` method: sets `keycloakUserId` and transitions status to ACTIVE (only valid from PENDING_APPROVAL)
- `suspend()`, `deactivate()` methods with valid state transition guards
- Unit tests: all state transitions, domain rule violations

---

## US-M1-004 — Domain Model: SystemRole & Permission

**As a** system architect,
**I want** `SystemRole` and `Permission` domain models,
**So that** the RBAC model is expressed in the domain layer with no infrastructure coupling.

**Acceptance Criteria:**
- `SystemRole`: id, name, roleType (RoleType: SYSTEM, CUSTOM), description
- `Permission`: id, resource (PermissionResource enum), action (PermissionAction enum), defaultScope (OrganizationScope enum)
- `RolePermission`: id, roleId, permissionId, grantedScope — represents system defaults
- `RolePermissionConfig`: id, tenantId, roleId, permissionId, grantedScope — represents tenant overrides
- No framework annotations in any of these classes
- Unit test: construct each, verify field integrity

---

## US-M1-005 — Domain Model: ProfileRoleAssignment & ApprovalRequest

**Acceptance Criteria:**
- `ProfileRoleAssignment`: id, identityProfileId, roleId, tenantId, branchId (nullable for tenant-wide assignments)
- `ApprovalRequest`: id, identityProfileId, requestedById, reviewedById (nullable), status (ApprovalStatus: PENDING, APPROVED, REJECTED), requestNotes, reviewNotes, requestedAt, reviewedAt
- `ApprovalRequest.approve(String reviewerId, String notes)` — transitions to APPROVED, sets reviewedById, reviewedAt
- `ApprovalRequest.reject(String reviewerId, String notes)` — transitions to REJECTED
- Cannot approve/reject an already-decided request (throws `ApprovalAlreadyDecidedException`)
- Unit tests: state machine for approval

---

## US-M1-006 — Domain Model: SessionRegistry

**Acceptance Criteria:**
- `SessionRegistry`: id, identityProfileId, tenantId, sessionToken (hashed), ipAddress, userAgent, status (SessionStatus: ACTIVE, EXPIRED, REVOKED), createdAt, expiresAt, lastActivityAt
- `revoke()` method: transitions ACTIVE → REVOKED
- `expire()` method: transitions ACTIVE → EXPIRED
- `isValid()` method: returns true only if ACTIVE and not past expiresAt
- Unit tests for each transition and `isValid()` edge cases

---

## US-M1-007 — JPA Entities

**As a** developer,
**I want** JPA entities corresponding to each domain model,
**So that** domain data persists to Postgres via the infrastructure layer.

**Acceptance Criteria:**
- One JPA entity per domain model, in `infrastructure/persistence/entity/`
- Each entity name: `{DomainClass}Entity` (e.g., `TenantEntity`, `BranchEntity`)
- Entity maps to its Liquibase-defined table exactly (no schema drift)
- All entities extend `BaseDomainEntity` (JPA version from `insurance-persistence`)
- MapStruct mappers in `infrastructure/persistence/adapter/` translate Entity ↔ Domain Model
- No business logic in entities — they are data containers only

---

## US-M1-008 — Spring Data Repositories

**As a** developer,
**I want** Spring Data JPA repositories for all entities,
**So that** the infrastructure layer can persist and query domain data through the out-port interfaces.

**Acceptance Criteria:**
- One repository interface per entity, extends `JpaRepository` + `JpaSpecificationExecutor`
- Repository interfaces are internal to infrastructure — domain out-ports are separate interfaces
- Domain out-ports (e.g., `TenantRepository`, `IdentityProfileRepository`) defined in `domain/port/out/`
- Infrastructure adapters (e.g., `TenantRepositoryAdapter`) implement domain out-ports and delegate to JPA repos
- Integration tests using Testcontainers (Postgres): basic CRUD + at least one Specification query per repo
- Tenant isolation: every query that should be tenant-scoped goes through `BaseSpecification` with `tenantId` predicate

---

## US-M1-009 — Application Boot Verification

**As a** developer,
**I want** the application to start successfully against the local docker-compose stack,
**So that** the M1 exit gate is verifiable end-to-end.

**Acceptance Criteria:**
- `mvn spring-boot:run` starts without errors
- All 15 Liquibase changelogs apply on startup
- Seed data is present in all relevant tables
- `/actuator/health` returns `{"status": "UP"}`
- Datasource, Redis, and Keycloak connections all healthy on startup

---

## M1 Exit Gate Summary
| Check | Method |
|---|---|
| `mvn clean install` passes | CI pipeline |
| 15 changelogs apply on fresh DB | Liquibase integration test |
| Seed data present | Repository integration test queries |
| All domain unit tests pass | `mvn test` domain module |
| All repository integration tests pass | `mvn verify` infra module (Testcontainers) |
| Coverage ≥ 80% domain module | JaCoCo report |
| Application boots + `/actuator/health` UP | Spring Boot test |
