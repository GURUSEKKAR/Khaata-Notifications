package com.khaata.service;

import com.khaata.dto.LoginRequest;
import com.khaata.dto.LoginResponse;
import com.khaata.dto.RegisterRequest;
import com.khaata.entity.AppUser;
import com.khaata.exception.BusinessRuleException;
import com.khaata.repository.AppUserRepository;
import com.khaata.security.JwtService;
import java.time.Instant;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Signup/login for shop accounts. The shop name is the login identifier (stored
 * normalized in {@code username}), so it must be unique. Passwords are BCrypt-hashed.
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

    /** Normalize a shop name into the login key (lowercase, trimmed, single spaces). */
    public static String toUsername(String shopName) {
        return shopName.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    @Transactional
    public LoginResponse register(RegisterRequest req) {
        String username = toUsername(req.shopName());
        if (userRepository.existsByUsername(username)) {
            throw new BusinessRuleException("A shop named '" + req.shopName().trim() + "' already exists. Choose another name.");
        }
        AppUser user = AppUser.builder()
                .username(username)
                .shopName(req.shopName().trim())
                .ownerName(req.ownerName().trim())
                .displayName(req.shopName().trim())
                .passwordHash(passwordEncoder.encode(req.password()))
                .role("OWNER")
                .createdAt(Instant.now())
                .build();
        userRepository.save(user);
        return new LoginResponse(jwtService.issue(username), user.getShopName(), user.getOwnerName());
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest req) {
        String username = toUsername(req.shopName());
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessRuleException("Invalid shop name or password."));
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new BusinessRuleException("Invalid shop name or password.");
        }
        return new LoginResponse(jwtService.issue(username), user.getShopName(), user.getOwnerName());
    }

    /** Verify a raw password against the stored user's hash (for sensitive actions). */
    @Transactional(readOnly = true)
    public boolean verifyPassword(String username, String rawPassword) {
        return userRepository.findByUsername(username)
                .map(u -> passwordEncoder.matches(rawPassword, u.getPasswordHash()))
                .orElse(false);
    }
}
