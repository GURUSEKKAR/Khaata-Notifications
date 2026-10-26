package com.khaata.service;

import com.khaata.dto.LoginRequest;
import com.khaata.dto.LoginResponse;
import com.khaata.dto.RegisterRequest;
import com.khaata.entity.AppUser;
import com.khaata.exception.BusinessRuleException;
import com.khaata.repository.AppUserRepository;
import com.khaata.security.JwtService;
import java.time.Instant;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verifies credentials against the stored BCrypt hash and issues a JWT.
 * A generic failure message avoids leaking whether the username exists.
 */
@Service
public class AuthService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(AppUserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest req) {
        AppUser user = userRepository.findByUsername(req.username().trim())
                .orElseThrow(() -> new BusinessRuleException("Invalid username or password."));

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new BusinessRuleException("Invalid username or password.");
        }

        String token = jwtService.issue(user.getUsername());
        return new LoginResponse(token, user.getUsername(), user.getDisplayName());
    }

    /**
     * Open registration: creates a new owner account and logs them in immediately.
     * Usernames are unique; the password is stored only as a BCrypt hash.
     */
    @Transactional
    public LoginResponse register(RegisterRequest req) {
        String username = req.username().trim();
        if (userRepository.existsByUsername(username)) {
            throw new BusinessRuleException("Username '" + username + "' is already taken.");
        }

        AppUser user = AppUser.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode(req.password()))
                .displayName(req.displayName() != null && !req.displayName().isBlank()
                        ? req.displayName().trim() : username)
                .role("OWNER")
                .createdAt(Instant.now())
                .build();
        userRepository.save(user);

        String token = jwtService.issue(user.getUsername());
        return new LoginResponse(token, user.getUsername(), user.getDisplayName());
    }

    /** Verify a raw password against a stored user's hash (used to confirm sensitive actions). */
    @Transactional(readOnly = true)
    public boolean verifyPassword(String username, String rawPassword) {
        return userRepository.findByUsername(username)
                .map(u -> passwordEncoder.matches(rawPassword, u.getPasswordHash()))
                .orElse(false);
    }
}
