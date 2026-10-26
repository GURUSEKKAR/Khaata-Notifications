package com.khaata.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Editable owner profile fields. Shop name and owner name stay required; phone,
 * email, and UPI are set here (phone/email optional, UPI needed before reminders).
 */
public record UpdateProfileRequest(
        @NotBlank(message = "shop name is required")
        @Size(min = 2, max = 120) String shopName,

        @NotBlank(message = "owner name is required")
        @Size(min = 2, max = 120) String ownerName,

        @Pattern(regexp = "^$|^\\+?[0-9]{10,15}$", message = "phone must be 10-15 digits")
        String phone,

        @Email(message = "email must be valid")
        String email,

        @Size(max = 80, message = "UPI id too long")
        String upiId
) {}
