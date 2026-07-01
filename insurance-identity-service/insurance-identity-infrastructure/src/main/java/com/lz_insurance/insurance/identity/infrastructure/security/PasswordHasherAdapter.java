package com.lz_insurance.insurance.identity.infrastructure.security;

import com.lz_insurance.insurance.identity.domain.port.out.PasswordHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Infrastructure adapter for {@link PasswordHasher}: delegates to the Spring Security
 * {@link PasswordEncoder} bean (BCrypt) supplied by {@code PasswordEncoderConfig} in
 * insurance-security-core. Thin by design — no logic beyond bridging the domain port to the encoder.
 */
@Component
@RequiredArgsConstructor
public class PasswordHasherAdapter implements PasswordHasher {

    private final PasswordEncoder passwordEncoder;

    @Override
    public String encrypt(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    @Override
    public boolean verify(String rawPassword, String hash) {
        return passwordEncoder.matches(rawPassword, hash);
    }
}
