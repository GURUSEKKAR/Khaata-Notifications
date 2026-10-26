package com.khaata.dto;

import jakarta.validation.constraints.NotBlank;

/** Password confirmation for sensitive actions (e.g. permanent account deletion). */
public record PasswordConfirmRequest(
        @NotBlank(message = "password confirmation is required")
        String password
) {}
