package com.khaata.dto;

import com.khaata.entity.CustomerType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

/**
 * Request to register a new customer. Validation runs at the API edge so bad
 * data never reaches the service or the database.
 *
 * <p>{@code dueWindowDays} is optional: if null, a sensible default is chosen
 * from the customer type (REGULAR = 30 days, NORMAL = 7 days).
 */
public record CreateCustomerRequest(
        @NotBlank(message = "name is required")
        String name,

        @NotBlank(message = "phone is required")
        @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "phone must be 10-15 digits, optional leading +")
        String phone,

        @Email(message = "email must be valid")
        String email,

        @NotNull(message = "customerType is required (REGULAR or NORMAL)")
        CustomerType customerType,

        @Positive(message = "dueWindowDays must be positive")
        Integer dueWindowDays,

        /** Optional Telegram chat id, if reminders should go over Telegram. */
        String telegramChatId
) {}
