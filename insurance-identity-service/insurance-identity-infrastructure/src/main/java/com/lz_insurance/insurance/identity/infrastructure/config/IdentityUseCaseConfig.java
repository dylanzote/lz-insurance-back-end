package com.lz_insurance.insurance.identity.infrastructure.config;

import com.lz_insurance.insurance.identity.domain.port.in.BootstrapTenantPort;
import com.lz_insurance.insurance.identity.domain.port.out.BranchRepository;
import com.lz_insurance.insurance.identity.domain.port.out.IdentityProfileRepository;
import com.lz_insurance.insurance.identity.domain.port.out.KeycloakUserPort;
import com.lz_insurance.insurance.identity.domain.port.out.PasswordHasher;
import com.lz_insurance.insurance.identity.domain.port.out.TenantRepository;
import com.lz_insurance.insurance.identity.domain.usecase.BootstrapTenantPortImpl;
import com.lz_insurance.security.core.config.PasswordEncoderConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Wires the framework-free domain use cases as Spring beans, injecting the real out-port adapters.
 * The domain expresses needs (constructor parameters); infrastructure satisfies them here. This is
 * the one place use-case implementations are instantiated — controllers depend only on the in-port
 * interfaces.
 *
 * <p>{@code @Import}s the standalone {@link PasswordEncoderConfig} (security-core, outside the scan
 * root) so the BCrypt {@code PasswordEncoder} behind {@code PasswordHasherAdapter} is available —
 * WITHOUT pulling in the full {@code SecurityConfig} stack (unwired until M3).
 */
@Configuration
@Import(PasswordEncoderConfig.class)
public class IdentityUseCaseConfig {

    @Bean
    BootstrapTenantPort bootstrapTenantUseCase(TenantRepository tenantRepository,
                                               BranchRepository branchRepository,
                                               IdentityProfileRepository identityProfileRepository,
                                               KeycloakUserPort keycloakUserPort,
                                               PasswordHasher passwordHasher) {
        return new BootstrapTenantPortImpl(tenantRepository, branchRepository,
                identityProfileRepository, keycloakUserPort, passwordHasher);
    }
}
