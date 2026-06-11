package com.lz_Insurance.testcontainers.annotation;

import com.lz_Insurance.testcontainers.factory.ContainerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TestcontainersConfig {

    @Bean
    @ConditionalOnProperty(name = "testcontainers.enabled", havingValue = "true", matchIfMissing = false)
    public ContainerFactory containerFactory() {
        return new ContainerFactory();
    }
}
