package com.lz_Insurance.persistence.config;

import com.lz_Insurance.security.core.service.CurrentUserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

/**
 * Enables Spring Data JPA auditing so {@code @CreatedBy}/{@code @LastModifiedBy}/
 * {@code @CreatedDate}/{@code @LastModifiedDate} on {@code BaseJpaEntity} are populated.
 * The auditor is the current authenticated user, falling back to {@code "SYSTEM"} for
 * system/bootstrap actions with no security context.
 *
 * <p>Repository scanning is intentionally NOT configured here — that is each bootable
 * service's responsibility (Spring Boot auto-configuration / {@code @DataJpaTest} slice).
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaAuditingConfig {

    @Bean
    public AuditorAware<String> auditorProvider(CurrentUserService currentUserService) {
        return () -> Optional.ofNullable(currentUserService.getCurrentUserId())
                .or(() -> Optional.of("SYSTEM"));
    }
}
