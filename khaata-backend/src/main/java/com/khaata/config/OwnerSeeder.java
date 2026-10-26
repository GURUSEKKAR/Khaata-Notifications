package com.khaata.config;

import com.khaata.entity.AppUser;
import com.khaata.repository.AppUserRepository;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Ensures a shop-owner login exists on first run, in every profile. The default
 * username/password come from configuration (env vars in prod) so the hash is never
 * hardcoded. Runs before the dev data seeder (@Order) so the account is always ready.
 */
@Slf4j
@Component
@Order(0)
public class OwnerSeeder implements CommandLineRunner {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String username;
    private final String password;
    private final String displayName;

    public OwnerSeeder(AppUserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       @Value("${khaata.security.owner-username:owner}") String username,
                       @Value("${khaata.security.owner-password:khaata123}") String password,
                       @Value("${khaata.security.owner-display-name:Shop Owner}") String displayName) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.username = username;
        this.password = password;
        this.displayName = displayName;
    }

    @Override
    public void run(String... args) {
        if (userRepository.existsByUsername(username)) {
            return;
        }
        userRepository.save(AppUser.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode(password))
                .displayName(displayName)
                .role("OWNER")
                .createdAt(Instant.now())
                .build());
        log.info("Seeded owner account '{}'. Change the default password in production.", username);
    }
}
