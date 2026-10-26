package com.khaata.dto;

import com.khaata.entity.TransactionType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A ledger entry enriched with the running balance *after* that entry, so the UI
 * history panel can show the balance evolving like a real passbook.
 */
public record LedgerEntryResponse(
        Long id,
        TransactionType type,
        BigDecimal amount,
        BigDecimal runningBalance,
        String note,
        LocalDate dueDate,
        boolean overdue,
        Instant createdAt
) {}
