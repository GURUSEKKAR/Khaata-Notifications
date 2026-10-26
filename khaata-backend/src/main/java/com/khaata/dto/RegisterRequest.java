package com.khaata.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** New-account signup payload. */
public record RegisterRequest(
        @NotBlank(message = "username is required")
        @Size(min = 3, max = 60, message = "username must be 3-60 characters")
        String username,

        @NotBlank(message = "password is required")
        @Size(min = 6, max = 72, message = "password must be 6-72 characters")
        String password,

        @Size(max = 120, message = "display name too long")
        String displayName
) {}
