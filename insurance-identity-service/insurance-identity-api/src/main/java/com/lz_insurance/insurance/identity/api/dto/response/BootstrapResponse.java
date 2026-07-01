package com.lz_insurance.insurance.identity.api.dto.response;

/**
 * Response body for a successful bootstrap: the server-generated ids the caller should store. Carries
 * NO credential — the temporary password is delivered to the admin out-of-band (welcome email,
 * deferred G-014); it is never echoed here.
 */
public record BootstrapResponse(
        String tenantId,
        String branchId,
        String identityProfileId,
        String message) {
}
