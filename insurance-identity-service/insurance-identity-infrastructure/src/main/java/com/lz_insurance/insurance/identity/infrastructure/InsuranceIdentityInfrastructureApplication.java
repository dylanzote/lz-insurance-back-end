package com.lz_insurance.insurance.identity.infrastructure;

import com.lz_insurance.persistence.config.JpaAuditingConfig;
import com.lz_insurance.web.advice.GlobalControllerAdvice;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

/**
 * Identity service entry point.
 *
 * <p><b>Auditing (G-009):</b> the shared {@link JpaAuditingConfig} lives in
 * {@code com.lz_insurance.persistence}, which is outside this app's
 * {@code com.lz_insurance.insurance} component-scan root, so it is wired explicitly via
 * {@code @Import} rather than scanned. This {@code @Import} is a deliberate isolation choice,
 * not a casing artifact (G-003 unified all packages to lowercase): broadening the scan to
 * {@code com.lz_insurance} would drag in the unconfigured Keycloak/security stack. It stays
 * targeted until M3 actually wires real security — the natural point to revisit @Import vs.
 * scan. Its auditor falls back to {@code "SYSTEM"} because {@code CurrentUserService} is not
 * in this M1 context.
 *
 * <p><b>Security:</b> Spring Security is on the classpath transitively (persistence →
 * security-core), but no auth is wired in M1. The servlet security auto-configurations are
 * excluded so Boot does not stand up a default login wall — {@code /actuator/health} and the
 * rest stay open until the Keycloak stack is wired in M3. The shared {@code SecurityConfig}
 * is deliberately NOT component-scanned (it sits outside the {@code com.lz_insurance.insurance}
 * scan root).
 *
 * <p><b>Redis:</b> {@code spring-boot-starter-data-redis} arrives transitively (security-core),
 * so Boot would auto-configure a Redis client + health indicator pointing at localhost — which
 * fails health (503) since no Redis is wired in M1. {@code DataRedisAutoConfiguration} is
 * excluded; Redis is introduced with session management in M3.
 *
 * <p><b>Error handling (M2):</b> the shared {@link GlobalControllerAdvice} lives in
 * {@code com.lz_insurance.web.advice}, outside the {@code com.lz_insurance.insurance} scan root, so
 * it is registered via the same targeted {@code @Import}. Without it, domain exceptions would fall
 * through to Boot's whitelabel page instead of the standard {@code ErrorResponse} shape (with the
 * correct 400/403/404/409 status per {@code ErrorCode}).
 */
@SpringBootApplication(exclude = {
        SecurityAutoConfiguration.class,
        ServletWebSecurityAutoConfiguration.class,
        ManagementWebSecurityAutoConfiguration.class,
        UserDetailsServiceAutoConfiguration.class,
        DataRedisAutoConfiguration.class
})
@ComponentScan(basePackages = {"com.lz_insurance.insurance"})
@Import({JpaAuditingConfig.class, GlobalControllerAdvice.class})
public class InsuranceIdentityInfrastructureApplication {

    public static void main(String[] args) {
        SpringApplication.run(InsuranceIdentityInfrastructureApplication.class, args);
    }

}
