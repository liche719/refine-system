package com.achobeta.refine.identity.account.infrastructure.security;

import com.achobeta.refine.identity.account.application.port.PasswordHasher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class BCryptPasswordHasher implements PasswordHasher {
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
    @Override public String hash(String rawPassword) { return encoder.encode(rawPassword); }
    @Override public boolean matches(String rawPassword, String passwordHash) { return encoder.matches(rawPassword, passwordHash); }
}
