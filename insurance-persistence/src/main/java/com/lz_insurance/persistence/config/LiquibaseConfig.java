package com.lz_insurance.persistence.config;

import liquibase.integration.spring.SpringLiquibase;
import org.springframework.boot.liquibase.autoconfigure.LiquibaseAutoConfiguration;
import org.springframework.boot.liquibase.autoconfigure.LiquibaseProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import javax.sql.DataSource;

@Configuration
@ConditionalOnProperty(prefix = "spring.liquibase", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import(LiquibaseAutoConfiguration.class)
@ConditionalOnMissingBean(SpringLiquibase.class)
public class LiquibaseConfig {

    @Bean
    public SpringLiquibase liquibase(DataSource dataSource, LiquibaseProperties properties) {
        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setDataSource(dataSource);
        liquibase.setChangeLog(properties.getChangeLog());
        liquibase.setShouldRun(properties.isEnabled());
        liquibase.setDefaultSchema(properties.getDefaultSchema());
        return liquibase;
    }
}
