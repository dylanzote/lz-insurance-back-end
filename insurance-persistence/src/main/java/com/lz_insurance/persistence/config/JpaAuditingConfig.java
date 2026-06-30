package com.lz_insurance.persistence.config;

import com.lz_insurance.security.core.service.CurrentUserService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

/**
 * Enables Spring Data JPA auditing so {@code @CreatedBy}/{@code @LastModifiedBy}/
 * {@code @CreatedDate}/{@code @LastModifiedDate} on {@code BaseJpaEntity} are populated.
 *
 * <p>The auditor resolves to the real authenticated user when a {@link CurrentUserService}
 * is present in the application context, and to {@code "SYSTEM"} when it is absent
 * (e.g. the M1 identity app, where the Keycloak/security stack is not yet wired) or when
 * the resolved user id is {@code null}. {@link CurrentUserService} is injected through an
 * {@link ObjectProvider} so this bean — and JPA auditing as a whole — can be activated
 * without dragging in the security stack before it exists.
 *
 * <p>Repository scanning is intentionally NOT configured here — that is each bootable
 * service's responsibility (Spring Boot auto-configuration / {@code @DataJpaTest} slice).
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaAuditingConfig {

    @Bean
    public AuditorAware<String> auditorProvider(ObjectProvider<CurrentUserService> currentUserServiceProvider) {
        return () -> {
            CurrentUserService currentUserService = currentUserServiceProvider.getIfAvailable();
            if (currentUserService == null) {
                return Optional.of("SYSTEM");
            }
            return Optional.ofNullable(currentUserService.getCurrentUserId())
                    .or(() -> Optional.of("SYSTEM"));
        };
    }
}
