package com.lz_insurance.insurance.identity.domain.dto;

/**
 * Outcome of a successful tenant bootstrap: the server-generated ids the caller should record —
 * the tenant, its headquarter branch, the TENANT_ADMIN identity profile, and the linked Keycloak
 * account.
 */
public record BootstrapResult(
        String tenantId,
        String branchId,
        String identityProfileId,
        String keycloakUserId) {
}
