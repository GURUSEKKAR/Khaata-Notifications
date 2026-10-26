package com.khaata.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Confirmation payload for deleting a customer. The owner must re-enter their own
 * login password — a standard "confirm sensitive action" guard against accidental
 * or unauthorized deletes.
 */
public record DeleteCustomerRequest(
        @NotBlank(message = "password confirmation is required")
        String password
) {}
