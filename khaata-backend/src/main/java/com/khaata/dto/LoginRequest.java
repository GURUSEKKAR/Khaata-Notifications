package com.khaata.dto;

import jakarta.validation.constraints.NotBlank;

/** Credentials posted to /api/auth/login — login is by shop name + password. */
public record LoginRequest(
        @NotBlank(message = "shop name is required") String shopName,
        @NotBlank(message = "password is required") String password
) {}
