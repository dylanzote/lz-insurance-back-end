# LZ Insurance — System Architecture

> This document is the canonical architecture reference.
> Load it in Claude Code with: `@docs/architecture.md`

---

## System Purpose
Multi-tenant SaaS insurance platform. LZ Insurance operates it for its own business (Canada + Cameroon launch markets) and licenses it to other insurance companies worldwide.

---

## Frontend Applications

| App | Technology   | Users |
|---|--------------|---|
| Mobile App | React Native | Policyholders, Claimants — iOS + Android |
| Customer Portal | React        | Policyholders, Claimants — web |
| Agent / Internal Dashboard | React        | Staff, Agents, Underwriters, Adjusters, Admins |
| Public Marketing Site | Astro        | Anonymous visitors, lead capture |

---

## Backend Services (Bounded Contexts)

### Core Platform Services

| Service | Responsibility                                                                           |
|---|------------------------------------------------------------------------------------------|
| `insurance-identity-service` | Identity, auth, RBAC, sessions, bootstrap                                                |
| `insurance-tenant-service` | Tenant onboarding, configuration, billing plan                                           |
| `insurance-customer-service` | Customer business profile, KYC, contact info (links to identity via `identityProfileId`) |
| `insurance-policy-service` | Policy lifecycle: quoting, binding, endorsement, renewal, cancellation                   |
| `insurance-claims-service` | Claims FNOL, workflow, adjustment, settlement                                            |
| `insurance-billing-service` | Premium calculation, invoicing, payment integration                                      |
| `insurance-document-service` | Document generation (policy docs, claim forms), storage, retrieval                       |
| `insurance-notification-service` | Email, SMS, push notifications(ios, android), webhook                                    |
| `insurance-audit-service` | Immutable audit log for all domain events                                                |

### Canadian-Specific Services(to be implemented for africa as well. that is what i will mostly sell in the african context. correct brainstorming for it)

| Service | Responsibility |
|---|---|
| `insurance-telematics-service` | UBI/telematics: ingest trip data from mobile, compute driving scores |
| `insurance-ubi-pricing-service` | Apply driving score to premium calculation (discount engine) |
| `insurance-regulatory-service` | Canadian regulatory reporting (OSFI, provincial regulators) |

### Africa / Cameroon-Specific Services

| Service | Responsibility |
|---|---|
| `insurance-mobile-money-service` | Mobile money payment integration (MTN MoMo, Orange Money) |
| `insurance-localization-service` | Multi-language (FR/EN), local product rules, local document formats |

---

## Shared Infrastructure Modules (Maven)

```
insurance-core                    # Utilities, base exceptions, shared value objects
insurance-web                     # HTTP filters, global error handling, response wrappers, request context
insurance-persistence             # BaseDomainEntity, BaseSpecification, tenant predicate injection
insurance-monitoring              # Actuator, metrics (Micrometer/Prometheus), distributed tracing
insurance-messaging               # Messaging abstractions (Kafka producer/consumer wrappers)
insurance-storage                 # File storage abstractions (S3-compatible)
insurance-security-core           # @RequiresPermission, TenantContext, SecurityConfig base
insurance-security-keycloak       # Keycloak JWT validation, realm routing, claims extraction
insurance-security-session        # Redis session tracking, session_registry Postgres table
insurance-security-multitenancy   # Tenant context filter, tenantId/branchId predicate injection
```

---

## Authentication Architecture

### Two Keycloak Realms

```
lz-insurance-internal
  - Hosts: staff, agents, underwriters, adjusters, admins
  - No self-registration (admin-invite only)
  - MFA enforced for TENANT_ADMIN and BRANCH_MANAGER roles
  - JWT claim: actorType=INTERNAL

lz-insurance-external
  - Hosts: policyholders, claimants, beneficiaries
  - Self-registration with email verification
  - Social login optional (future)
  - JWT claim: actorType=EXTERNAL
```

### JWT Token Claims (Standard + Custom)
```json
{
  "sub": "keycloak-user-id",
  "tenantId": "tenant-uuid",
  "branchId": "branch-uuid",
  "actorType": "INTERNAL",
  "internalUserType": "AGENT",
  "roles": ["AGENT"],
  "permissions": ["policy:read:OWN", "claim:read:BRANCH"],
  "sessionId": "session-uuid"
}
```

---

## Permission Model

### Structure
`resource:action:scope`

**Resources:** POLICY, CLAIM, REPORT, USER, BRANCH, TENANT, PERMISSION, SESSION, AUDIT, TELEMATICS

**Actions:** CREATE, READ, UPDATE, DELETE, APPROVE, REJECT, EXPORT, CONFIGURE

**Scopes:**
- `OWN` — data created by / belonging to the requesting user
- `BRANCH` — data within the user's assigned branch
- `ALL` — entire tenant's data (admin-level)

### Resolution at Runtime
1. Extract `roles` from JWT
2. Load system defaults from `role_permissions` table (cached in Redis)
3. Merge tenant overrides from `role_permission_configs` table (tenant admin customizations)
4. Override wins on conflict
5. Resolved permission set validated by `@RequiresPermission` interceptor

### Scope Enforcement
Scope is enforced at the JPA Specification layer only. Never in service/use case code.
- `OWN` → `WHERE created_by = :identityProfileId`
- `BRANCH` → `WHERE branch_id = :branchId`
- `ALL` → no additional predicate

---

## Multi-Tenancy Model

```
Tenant
  └── HeadquarterBranch
        ├── Branch A
        │     ├── Sub-Branch A1
        │     └── Sub-Branch A2
        └── Branch B
```

- Every row in every business table has `tenant_id` and optionally `branch_id`
- `TenantContext` (thread-local) is populated by `insurance-security-multitenancy` filter on every request
- `BaseSpecification` automatically injects `tenant_id = :tenantId` on every query
- A staff member with `OrganizationScope=BRANCH` physically cannot retrieve another branch's records

---

## UBI / Telematics (Canadian Feature) Cameroon features needs to be done

### Flow
```
Mobile App (React Native)
  → Trip recorded locally
  → POST /api/v1/trips → insurance-telematics-service
  → Trip stored + scoring job triggered
  → Scoring engine evaluates: speed compliance, hard braking, cornering, time-of-day
  → DrivingScore updated on CustomerProfile
  → Event emitted → insurance-ubi-pricing-service
  → Premium discount recalculated on next renewal
```

### Scoring Factors
| Factor | Weight |
|---|---|
| Speed compliance | 30% |
| Hard braking events | 25% |
| Cornering smoothness | 20% |
| Time of day (night driving penalty) | 15% |
| Trip distance/frequency | 10% |

### Score → Discount Mapping (configurable per tenant)
| Score Range | Max Discount |
|---|---|
| 90–100 | Up to 25% |
| 75–89 | Up to 15% |
| 60–74 | Up to 8% |
| < 60 | No discount |

---

## Service Communication

- **Sync (REST):** API gateway → services, service-to-service for real-time reads
- **Async (Messaging):** domain events for eventual consistency (policy bound → notify customer, claim filed → trigger audit)
- **API Gateway:** routes external traffic, enforces JWT validation at edge
- **Service Discovery:** Kubernetes DNS (production) / Docker Compose hostnames (dev)

---

## Data Residency
- Canadian customers: data must reside in Canadian AWS/GCP region
- Cameroonian customers: data in closest African region (initially same cluster, isolated by tenant)
- Future: region-per-country for full data sovereignty

---

## Infrastructure (Dev / Local)
```
docker-compose stack:
  - postgres:15 (port 5432)
  - redis:7 (port 6379)
  - keycloak:23 (port 8080)
  - kafka + zookeeper (port 9092)
  - mailhog (SMTP trap, port 8025)
```
