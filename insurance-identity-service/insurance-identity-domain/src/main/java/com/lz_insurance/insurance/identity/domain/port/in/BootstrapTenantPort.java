package com.lz_insurance.insurance.identity.domain.port.in;

import com.lz_insurance.core.context.RequestContext;
import com.lz_insurance.insurance.identity.domain.dto.BootstrapResult;
import com.lz_insurance.insurance.identity.domain.dto.CreateAdminTenantRequest;

/**
 * In-port for the one-time tenant bootstrap ceremony (US-M2-002 / UJ-001): atomically creates a new
 * tenant, its headquarter branch and the first TENANT_ADMIN.
 *
 * <p>Implemented framework-free in the domain ({@code usecase} package) and wired as a bean by the
 * infrastructure {@code IdentityUseCaseConfig}. Per the M2+ standard, the use case receives the
 * {@link RequestContext} as an explicit parameter (audit attribution + correlation id) — it never
 * reads a thread-local.
 */
public interface BootstrapTenantPort {

    BootstrapResult createTenantAdmin(CreateAdminTenantRequest request, RequestContext context);
}
