# insurance-identity-service — Service Context

> This file is loaded when Claude works in the `insurance-identity-service` directory.
> Root `CLAUDE.md` is always loaded first — this file adds service-specific depth.

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

## Maven Modules
```
insurance-identity-service/
  insurance-identity-service-api/
    com.lz_insurance.insurance.identity.api
      controller/         # REST controllers (incoming adapters) ionterfaces
      usecase/            # Use case implementations service of the controllers
      dto/                # Request/Response DTOs
      mapper/             # DTO ↔ Domain mappers
      openapi/            # OpenAPI config
  insurance-identity-service-domain/
    com.lz_insurance.insurance.identity.domain
      model/              # Domain entities 
      port/
        in/               # Use case interfaces (called by API)
        out/              # Repository/external service interfaces (implemented by infra)
      usecase/            # Use case implementations
      support/            # support classes for usecases to avoid god classes
      event/              # Domain events
      exception/          # Domain exceptions
      enum/               # Domain enumerations
  insurance-identity-service-infrastructure/
    com.lz_insurance.insurance.identity.infrastructure
      persistence/
        entity/           # JPA entities
        repository/       # Spring Data JPA repos
        adapter/          # Implements domain out-ports
        specification/    # JPA Specifications
      keycloak/           # Keycloak adapter (implements identity out-ports)
      redis/              # Session/cache adapter
      config/             # Spring config, security config
```

---

## Domain Enums (already implemented)
- `ActorType`: INTERNAL, EXTERNAL
- `IdentityStatus`: PENDING_APPROVAL, ACTIVE, SUSPENDED, DEACTIVATED
- `InternalUserType`: STAFF, AGENT, UNDERWRITER, CLAIMS_ADJUSTER, BRANCH_MANAGER, TENANT_ADMIN
- `ExternalUserType`: POLICYHOLDER, CLAIMANT, BENEFICIARY
- `OrganizationScope`: OWN, BRANCH, ALL
- `ApprovalStatus`: PENDING, APPROVED, REJECTED
- `SessionStatus`: ACTIVE, EXPIRED, REVOKED
- `PermissionAction`: CREATE, READ, UPDATE, DELETE, APPROVE, REJECT, EXPORT, CONFIGURE
- `PermissionResource`: POLICY, CLAIM, REPORT, USER, BRANCH, TENANT, PERMISSION, SESSION, AUDIT
- `RoleType`: SYSTEM (immutable defaults), CUSTOM (tenant-created)

---

## Domain Model (M1 — to implement)
Core entities in `domain/model/`:

| Class | Key Fields |
|---|---|
| `Tenant` | id, name, code, country, bootstrapped, headquarterBranchId, status |
| `Branch` | id, tenantId, parentBranchId, name, code, type, status |
| `IdentityProfile` | id, tenantId, branchId, actorType, internalUserType/externalUserType, keycloakUserId, status |
| `SystemRole` | id, name, roleType, description |
| `Permission` | id, resource, action, defaultScope |
| `RolePermission` | id, roleId, permissionId, grantedScope (junction — system defaults) |
| `RolePermissionConfig` | id, tenantId, roleId, permissionId, grantedScope (tenant overrides) |
| `ProfileRoleAssignment` | id, identityProfileId, roleId, tenantId, branchId |
| `ApprovalRequest` | id, identityProfileId, requestedById, reviewedById, status, notes |
| `SessionRegistry` | id, identityProfileId, tenantId, sessionToken, status, createdAt, expiresAt |

All extend `BaseDomainEntity` from `insurance-persistence`.

---

## Liquibase Changelog Status (M1 — COMPLETED)
All 15 changelogs written and structured:
- `001` tenants table
- `002` branches table
- `003` identity_profiles table
- `004` system_roles table
- `005` permissions table
- `006` role_permissions table
- `007` role_permission_configs table
- `008` profile_role_assignments table
- `009` approval_requests table
- `010` session_registry table
- `011–015` seed data (roles, permissions, role-permission matrix)

**Pre-production rule:** edit in place, do not add new changesets for corrections.

---

## Current Milestone: M1 — Foundation

### Completed
- [x] Maven module structure
- [x] Domain enums
- [x] Liquibase changelog layer (all 15 files)

### In Progress / Next
- [ ] Domain model classes (Tenant, Branch, IdentityProfile, SystemRole, Permission, etc.)
- [ ] JPA entities (mirror domain models, mapped to DB schema)
- [ ] Spring Data repositories (with Specification support)
- [ ] Domain port interfaces (in/ and out/)
- [ ] M1 exit gate: application boots, schema applied, seed data loaded, repositories pass integration tests

### M1 Exit Gate (do not advance to M2 until ALL pass)
1. `mvn clean install` passes with zero errors
2. Liquibase applies all 15 changelogs cleanly on fresh Postgres
3. Seed data present and queryable
4. All repository integration tests pass (Testcontainers)
5. Code coverage ≥ 80% on domain module

---

## M1 User Stories Reference
Load when needed: `@docs/identity-service/m1-user-stories.md`

---

## Key Implementation Rules for This Service
- `IdentityProfile` is the aggregate root for identity — all operations go through it
- Bootstrap endpoint (`POST /internal/bootstrap`) must check `tenant.bootstrapped` before acting — idempotency guard
- Approval workflow: branch managers approve internal registrations BEFORE Keycloak account is created
- Never create Keycloak user until `ApprovalRequest` is in `APPROVED` state
- `RolePermissionConfig` overrides are merged on top of `RolePermission` defaults at resolution time — defaults win if no override exists
- All queries MUST go through Specification layer — never raw JPQL with tenant/branch filters hardcoded
