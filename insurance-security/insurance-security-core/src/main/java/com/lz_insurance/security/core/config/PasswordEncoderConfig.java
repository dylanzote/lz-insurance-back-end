package com.lz_insurance.security.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Standalone {@link PasswordEncoder} bean — deliberately SEPARATE from {@link SecurityConfig}.
 *
 * <p>{@code SecurityConfig} is a full {@code @EnableWebSecurity}/{@code @EnableMethodSecurity} chain
 * (JWT decoder, session-validation filter, resource server) that services deliberately keep
 * unwired until M3. Password hashing, however, is needed NOW (e.g. tenant bootstrap stores a
 * BCrypt hash of the generated credential). Isolating the encoder here lets a service import just
 * the hashing bean without dragging in the whole security stack.
 */
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
