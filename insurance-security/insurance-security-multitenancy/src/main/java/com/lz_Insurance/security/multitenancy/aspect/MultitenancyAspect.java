package com.lz_Insurance.security.multitenancy.aspect;

import com.lz_Insurance.security.multitenancy.annotation.TenantScoped;
import com.lz_Insurance.security.multitenancy.context.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class MultitenancyAspect {

    @Around("@annotation(tenantScoped)")
    public Object aroundTenantScoped(ProceedingJoinPoint joinPoint, TenantScoped tenantScoped) throws Throwable {
        String tenantId = TenantContext.getTenantId();
        
        if (tenantId == null) {
            log.warn("No tenant context found for method: {}", joinPoint.getSignature().toShortString());
            throw new IllegalStateException("Tenant context not set");
        }
        
        log.debug("Executing tenant-scoped operation for tenant: {}", tenantId);
        
        try {
            return joinPoint.proceed();
        } catch (Exception e) {
            log.error("Error executing tenant-scoped operation for tenant: {}", tenantId, e);
            throw e;
        }
    }
}
