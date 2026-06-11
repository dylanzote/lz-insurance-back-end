package com.lz_Insurance.monitoring.health;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Component
@RequiredArgsConstructor
public class DatabaseHealthIndicator implements HealthIndicator {

    private final DataSource dataSource;

    @Override
    public Health health() {
        try {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            
            if (result != null && result == 1) {
                return Health.up()
                    .withDetail("database", "PostgreSQL")
                    .withDetail("status", "Connected")
                    .build();
            }
            
            return Health.down()
                .withDetail("database", "PostgreSQL")
                .withDetail("status", "Invalid response")
                .build();
        } catch (Exception e) {
            return Health.down()
                .withDetail("database", "PostgreSQL")
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
