package com.khaata.controller;

import com.khaata.dto.PasswordConfirmRequest;
import com.khaata.dto.ProfileResponse;
import com.khaata.dto.UpdateProfileRequest;
import com.khaata.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The logged-in owner's profile: view, update, and permanent account deletion. */
@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public ProfileResponse get() {
        return profileService.get();
    }

    @PutMapping
    public ProfileResponse update(@Valid @RequestBody UpdateProfileRequest req) {
        return profileService.update(req);
    }

    /** Permanently delete the account + all its data. Requires the owner's password. */
    @DeleteMapping
    public ResponseEntity<Void> deleteAccount(@Valid @RequestBody PasswordConfirmRequest req) {
        profileService.deleteAccount(req.password());
        return ResponseEntity.noContent().build();
    }
}
