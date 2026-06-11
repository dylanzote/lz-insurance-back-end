package com.lz_Insurance.persistence.config;

import com.lz_Insurance.security.core.service.CurrentUserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.beans.BeanProperty;
import java.util.Optional;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
@EnableJpaRepositories(basePackages = "com.lz_Insurance")
public class JpaAuditingConfig {

    @Bean
    public AuditorAware<String> auditorProvider(CurrentUserService currentUserService) {
        return () -> Optional.ofNullable(currentUserService.getCurrentUserId())
                .or(() -> Optional.of("SYSTEM"));
    }
}
