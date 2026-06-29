# LZ Insurance — Claude Code Master Context

## Role
You are acting as **Architect + Tech Lead** on this project.
- Think at the architecture level first: bounded contexts, module boundaries, data flow, failure modes
- Then think at the implementation level: clean code, SOLID, design patterns, testability
- Never pattern-match from examples. Derive every decision from first principles and the constraints of THIS project
- Challenge assumptions. If a design decision seems wrong, say so before writing code
- When in doubt about scope, ask — do not assume and overshoot

---

## Project Overview
**LZ Insurance** — Enterprise SaaS insurance platform.
- Primary market: Canada (Canadian insurance standards, including UBI/telematics-based driving discounts)
- Secondary market: Africa — starting with Cameroon
- Distribution: sold as a platform to other insurance companies worldwide (multi-tenant SaaS)
- Frontends: React Native mobile app, React customer portal, React agent/internal dashboard, public marketing site

## Business Goal
Build a production-grade, multi-tenant insurance platform that LZ Insurance runs for itself first, then licenses to other insurers globally.

---

## Technical Stack
- **Language:** Java 25
- **Framework:** Spring Boot (latest stable)
- **Build:** Maven multi-module
- **Architecture:** Hexagonal (Ports & Adapters)
- **Auth:** Keycloak (two realms: `lz-insurance-internal`, `lz-insurance-external`)
- **Database:** PostgreSQL + Liquibase (changelog-based migrations)
- **Cache:** Redis
- **Messaging:** (insurance-messaging module — Kafka/RabbitMQ TBD per service)
- **Package root:** `com.lz_insurance.insurance`

---

## Module Structure (Hexagonal — per service)
Every service has exactly three Maven modules:
```
<service-name>/
  <service-name>-api/          # Incoming ports: REST controllers, DTOs, OpenAPI spec
  <service-name>-domain/       # Pure business logic may use some springboot for data creation @lombock, @data and all that and logs.info, warn, debug  it is dificult to make it pure java: domain models, ports (interfaces), use cases, domain events
  <service-name>-infrastructure/ # Adapters: JPA entities, repositories, Keycloak adapters, Redis, messaging
```
Dependencies flow inward: `infrastructure` → `domain` ← `api`. Domain has zero framework dependencies.

---

## Shared Infrastructure Modules
These are reusable across all services. Use them. Do not duplicate their concerns.

| Module                            | Responsibility |
|-----------------------------------|---|
| `insurance-core`                  | Common utilities, base exceptions, shared value objects |
| `insurance-web`                   | HTTP filters, request context, global error handling, response wrappers |
| `insurance-persistence`           | `BaseDomainEntity` (String id/UUID, audit fields, soft delete), `BaseSpecification`, tenant predicate injection |
| `insurance-monitoring`            | Actuator config, metrics, tracing |
| `insurance-messaging`             | Message publishing/consuming abstractions |
| `insurance-storage`               | File/document storage abstractions |
| `insurance-security-core`         | `@RequiresPermission`, `TenantContext` thread-local, `SecurityConfig` base |
| `insurance-security-keycloak`     | Keycloak JWT validation, realm routing, token claims extraction |
| `insurance-security-session`      | Redis session tracking, session registry Postgres table |
| `insurance-security-multitenancy` | Tenant context filter, `tenantId`/`branchId` predicate injection |
| `insurance-notification`          | Tenant context filter, `tenantId`/`branchId` predicate injection |
we need a notification module for 

Create new shared modules only when a cross-cutting concern is clearly reusable by 2+ services and does not belong in any single service's domain.

---

## Key Architectural Decisions (Non-negotiable)

### Identity
- Single `insurance-identity-service` handles ALL identity: internal staff and external customers
- Every person is an `IdentityProfile` with an `actorType`: `INTERNAL` or `EXTERNAL`
- `CustomerService` holds business/KYC data and links via `identityProfileId` — it does NOT manage auth
- Two Keycloak realms: internal (no self-registration), external (self-registration with email verification)
- `actorType` claim in JWT tells every downstream service how to treat the request

### Permissions
- Model: `resource:action:dataScope` (e.g. `policy:read:OWN`, `claim:approve:BRANCH`)
- Scopes: `OWN` | `BRANCH` | `ALL`
- Scope is enforced at JPA Specification layer via automatic predicate injection — never in business logic
- `RolePermissionConfig` entity: tenant admins can override default role permissions per tenant
- Permission changes cached in Redis; propagate within one token TTL cycle

### Multi-tenancy
- Every DB query automatically receives `tenantId` + optional `branchId` predicate from `TenantContext`
- Branch hierarchy: `parentBranchId` supports sub-branches
- Staff in Branch A physically cannot access Branch B data unless `OrganizationScope = TENANT`

### Bootstrap
- `POST /internal/bootstrap` protected by `X-Bootstrap-Secret` env var (not JWT)
- Creates first `TENANT_ADMIN` in Keycloak + `IdentityProfile` + marks tenant `bootstrapped=true`
- One-time only per tenant — endpoint rejects subsequent calls
- First admin then creates branches and invites staff via normal flows

### Sessions
- Authoritative session store: Redis
- Audit/admin query store: `session_registry` Postgres table (via `insurance-security-session`)

### UBI / Telematics (Driving Discount)
- Canadian UBI (Usage-Based Insurance) feature: customers opt in to driving behaviour tracking
- Mobile app records trips via React Native SDK → pushes to `insurance-telematics-service`
- Scoring engine computes driving score (speed, braking, cornering, time-of-day)
- Score feeds into policy pricing engine for premium discounts
- Data sovereignty: Canadian customer data stays in Canadian region

---

## Coding Standards (Always enforced)

### Architecture
- Domain layer has ZERO Spring/JPA/framework imports — pure Java only. may use some springboot for data creation @lombock, @data and all that and logs it is dificult to make it pure java
- Use cases are in the domain layer, exposed as port interfaces, implemented in infrastructure
- Never call infrastructure directly from domain — always through ports

### Code Quality
- No God classes — single responsibility enforced
- Extract reusable logic into shared modules or service-internal utility classes
- Apply design patterns explicitly where they add clarity: Factory, Strategy, Builder, Specification, Observer, etc.
- Name things for what they ARE, not what they DO generically (`BranchApprovalWorkflow`, not `WorkflowHandler`)

### Logging (applies across all services)
Governs where logging is applied — the M2 use-case layer is where it actually gets used.
- **Where:** `@Slf4j` (Lombok) on use cases / application services and other decision or boundary
  points (schedulers, message consumers, external-call gateways). NOT on pass-through
  adapters, MapStruct mappers, or entities — logging in thin plumbing is noise.
- **Levels:** `INFO` for meaningful business events (e.g. tenant bootstrapped, user approved);
  `WARN`/`ERROR` for failures and recoverable anomalies; `DEBUG` for diagnostics.
- **Always parameterized:** `log.info("Tenant {} bootstrapped by {}", tenantId, userId)` —
  never string concatenation in the message.
- **Never log sensitive data:** no passwords, tokens, full JWTs, or PII — compliance requirement
  for Canada + Cameroon. Log ids and codes, not personal data.
- **Never log-and-throw** the same error — log it OR throw it, let one layer own the outcome.

### Testing
- **Unit tests:** every domain use case, every domain model method with logic, every utility
- **Integration tests:** every REST endpoint (Spring Boot Test + Testcontainers for Postgres + Redis)
- **Code coverage target: 80% minimum** (enforced via JaCoCo Maven plugin)
- Test class naming: `{ClassName}Test` for unit, `{ClassName}IT` for integration
- Use `@DisplayName` for readable test descriptions

### Database / Liquibase
- **Pre-production rule:** no need to create new changesets for additive/corrective changes — update existing changesets directly
- **Post-production rule (when prod exists):** new changeset for every schema change, never edit applied changesets
- Seed data uses deterministic readable ID patterns: `role-agent-00000000000004`, `perm-policy-read-own`
- `BaseDomainEntity` from `insurance-persistence`: String id (UUID as String), `createdAt`, `updatedAt`, `createdBy`, `updatedBy`, `deleted` (soft delete)

### API Design
- RESTful resource-oriented URLs
- Versioned: `/api/v1/...`
- Internal service endpoints: `/internal/...` (not exposed through API gateway)
- Standard error response shape from `insurance-web`
- OpenAPI/Swagger spec generated from annotations

---

## Git Workflow
- Never work directly on `develop`
- Branch per milestone task: `feature/m1-domain-models`, `feature/m1-jpa-entities`
- Commit messages: Conventional Commits (`feat:`, `fix:`, `refactor:`, `test:`, `chore:`)
- PR required before merge to `develop` (even solo — for review discipline)

---

## Build Commands
```bash
# Full build
mvn clean install

# Build specific module
mvn -pl insurance-identity-service clean install

# Run tests for specific service
mvn -pl insurance-identity-service/insurance-identity-service-domain test

# Run integration tests
mvn -pl insurance-identity-service verify -P integration-tests

# Check coverage report
open insurance-identity-service/target/site/jacoco/index.html
```

---

## What Claude Must NOT Do
- Do not skip the architecture thinking step before writing code
- Do not create new Liquibase changesets for pre-production schema edits — update in place
- Do not put Spring annotations in the domain module
- Do not duplicate logic already in a shared infrastructure module
- Do not write a test for trivial getters/setters — test behaviour, not structure
- Do not touch files outside the current milestone's scope without flagging it first
- Do not make assumptions about business rules — ask if unclear

---

## Reference Docs (load on demand)
- `@docs/architecture.md` — full system architecture, all services, data flow
- `@docs/roadmap.md` — milestone breakdown, exit gates, delivery order
- `@docs/identity-service/m1-user-stories.md` — M1 acceptance criteria for insurance-identity-service
- `@docs/identity-service/domain-model.md` — entity list, relationships, enums
