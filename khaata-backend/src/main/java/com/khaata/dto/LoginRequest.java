package com.khaata.dto;

import jakarta.validation.constraints.NotBlank;

/** Credentials posted to /api/auth/login. */
public record LoginRequest(
        @NotBlank(message = "username is required") String username,
        @NotBlank(message = "password is required") String password
) {}
