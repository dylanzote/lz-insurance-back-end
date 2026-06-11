package com.lz_Insurance.web.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private static final String[] ALLOWED_ORIGINS = {
        "http://localhost:3000",
        "http://localhost:8080",
        "http://localhost:4200"
    };

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(ALLOWED_ORIGINS)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addFormatterForFieldType(LocalDate.class, new LocalDateFormatter());
    }

    private static class LocalDateFormatter implements org.springframework.format.Formatter<LocalDate> {
        private final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;

        @Override
        public LocalDate parse(String text, java.util.Locale locale) {
            return LocalDate.parse(text, formatter);
        }

        @Override
        public String print(LocalDate object, java.util.Locale locale) {
            return object.format(formatter);
        }
    }
}
