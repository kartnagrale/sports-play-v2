package com.neml.badminton.seed;

import com.neml.badminton.entity.Role;
import com.neml.badminton.entity.User;
import com.neml.badminton.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Explicit, one-time bootstrap for a fresh installation. It is disabled unless
 * the operator supplies APP_BOOTSTRAP_ENABLED=true and all credentials.
 */
@Component
@ConditionalOnProperty(name = "app.bootstrap.enabled", havingValue = "true")
public class SuperAdminBootstrap implements CommandLineRunner {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final String email;
    private final String password;
    private final String fullName;

    public SuperAdminBootstrap(UserRepository users, PasswordEncoder encoder,
            @Value("${app.bootstrap.email:}") String email,
            @Value("${app.bootstrap.password:}") String password,
            @Value("${app.bootstrap.full-name:Platform Super Admin}") String fullName) {
        this.users = users; this.encoder = encoder; this.email = email;
        this.password = password; this.fullName = fullName;
    }

    @Override @Transactional
    public void run(String... args) {
        if (email.isBlank() || password.length() < 12) {
            throw new IllegalStateException("Bootstrap requires an email and a password of at least 12 characters");
        }
        String normalized = email.toLowerCase().trim();
        users.findByEmail(normalized).ifPresentOrElse(existing -> {
            if (existing.getRole() != Role.SUPER_ADMIN) {
                throw new IllegalStateException("Bootstrap email already belongs to a non-super-admin user");
            }
        }, () -> users.save(User.builder().email(normalized).passwordHash(encoder.encode(password))
                .fullName(fullName.trim()).role(Role.SUPER_ADMIN).build()));
    }
}
