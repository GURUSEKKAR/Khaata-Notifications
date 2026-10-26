package com.khaata.dto;

import com.khaata.entity.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Request to record a ledger event — either giving credit or recording a repayment.
 * The amount must be strictly positive; the sign is implied by {@link #type}.
 */
public record CreateTransactionRequest(
        @NotNull(message = "type is required (CREDIT or REPAYMENT)")
        TransactionType type,

        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.01", message = "amount must be greater than 0")
        BigDecimal amount,

        @Size(max = 255, message = "note too long")
        String note,

        /**
         * Optional explicit due date for a CREDIT. If omitted, the service computes it
         * from the customer's due window (createdDate + dueWindowDays).
         */
        java.time.LocalDate dueDate
) {}
