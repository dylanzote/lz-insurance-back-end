package com.lz_insurance.security.multitenancy.filter;

import com.lz_insurance.security.multitenancy.context.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class TenantIdentificationFilter extends OncePerRequestFilter {

    private static final String TENANT_HEADER = "X-Tenant-ID";
    private static final String DEFAULT_TENANT = "default";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                   HttpServletResponse response,
                                   FilterChain filterChain) throws ServletException, IOException {
        String tenantId = request.getHeader(TENANT_HEADER);
        
        if (tenantId == null || tenantId.isBlank()) {
            tenantId = DEFAULT_TENANT;
        }
        
        TenantContext.setTenantId(tenantId);
        
        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
