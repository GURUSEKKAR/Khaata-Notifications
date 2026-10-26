package com.khaata.security;

import com.khaata.entity.AppUser;
import com.khaata.exception.NotFoundException;
import com.khaata.repository.AppUserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Resolves the currently authenticated owner from the security context. The JWT
 * filter stores the username as the principal; this maps it to the AppUser row so
 * services can scope data to the logged-in owner.
 */
@Component
public class CurrentUser {

    private final AppUserRepository userRepository;

    public CurrentUser(AppUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public AppUser get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new NotFoundException("No authenticated user.");
        }
        return userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new NotFoundException("Authenticated user not found: " + auth.getName()));
    }

    public Long id() {
        return get().getId();
    }
}
