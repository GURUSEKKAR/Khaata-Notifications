package com.khaata.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** New-account signup: shop name + owner name + password. */
public record RegisterRequest(
        @NotBlank(message = "shop name is required")
        @Size(min = 2, max = 120, message = "shop name must be 2-120 characters")
        String shopName,

        @NotBlank(message = "owner name is required")
        @Size(min = 2, max = 120, message = "owner name must be 2-120 characters")
        String ownerName,

        @NotBlank(message = "password is required")
        @Size(min = 6, max = 72, message = "password must be 6-72 characters")
        String password
) {}
