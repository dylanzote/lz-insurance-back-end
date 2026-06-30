package com.lz_insurance.monitoring.config;

import io.micrometer.tracing.Tracer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TracingConfig {

    @Bean
    public TracerCustomizer tracerCustomizer(Tracer tracer) {
        return new TracerCustomizer(tracer);
    }

    public static class TracerCustomizer {
        private final Tracer tracer;

        public TracerCustomizer(Tracer tracer) {
            this.tracer = tracer;
        }

        public Tracer getTracer() {
            return tracer;
        }
    }
}
