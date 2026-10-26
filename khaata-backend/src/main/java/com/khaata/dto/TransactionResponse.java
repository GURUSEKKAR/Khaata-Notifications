package com.khaata.dto;

import com.khaata.entity.TransactionType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** A single ledger entry as returned to the UI. */
public record TransactionResponse(
        Long id,
        Long customerId,
        TransactionType type,
        BigDecimal amount,
        String note,
        LocalDate dueDate,
        Instant createdAt
) {}
