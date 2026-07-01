package com.lz_insurance.insurance.identity.api.usecase;

import com.lz_insurance.core.context.RequestContext;
import com.lz_insurance.core.exception.ErrorCode;
import com.lz_insurance.core.exception.InsuranceException;
import com.lz_insurance.insurance.identity.api.controller.BootstrapApi;
import com.lz_insurance.insurance.identity.api.dto.request.BootstrapRequest;
import com.lz_insurance.insurance.identity.api.dto.response.BootstrapResponse;
import com.lz_insurance.insurance.identity.domain.dto.BootstrapResult;
import com.lz_insurance.insurance.identity.domain.port.in.BootstrapTenantPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * API-layer service backing {@link BootstrapApi}. Validates the break-glass {@code X-Bootstrap-Secret}
 * (this endpoint runs before any JWT exists, so the shared secret is its only auth), then delegates to
 * the domain in-port. {@link RequestContext} is injected by the argument resolver and passed through —
 * the service never touches the thread-local holder. Logs the tenant CODE only (never the PII-bearing
 * request body).
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class BootstrapService implements BootstrapApi {

    private final BootstrapTenantPort bootstrapTenantPort;

    @Value("${identity.bootstrap.secret}")
    private String bootstrapSecret;

    @Override
    public BootstrapResponse bootstrap(String secret, BootstrapRequest request, RequestContext context) {
        validateSecret(secret, request.tenantCode());
        log.info("Bootstrap request accepted for tenant code {} [correlationId={}]", request.tenantCode(), context.correlationId());
        BootstrapResult result = bootstrapTenantPort.createTenantAdmin(BootstrapRequest.toDomain(request), context);
        return new BootstrapResponse(result.tenantId(), result.branchId(), result.identityProfileId(), "Tenant bootstrapped; TENANT_ADMIN created and must change password at first login");
    }

    private void validateSecret(String secret, String tenantCode) {
        if (secret == null || secret.isBlank() || !secret.equals(bootstrapSecret)) {
            log.warn("Rejected bootstrap for tenant code {}: invalid bootstrap secret", tenantCode);
            throw new InsuranceException(ErrorCode.FORBIDDEN, "Invalid bootstrap secret");
        }
    }
}
