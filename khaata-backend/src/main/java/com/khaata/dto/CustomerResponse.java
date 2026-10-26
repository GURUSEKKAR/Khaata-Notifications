package com.khaata.dto;

import com.khaata.entity.CustomerType;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Customer view returned to the UI. Combines the stored customer fields with the
 * derived money figures (outstanding / paid / overdue) so the right-hand customer
 * list can show balances without extra round-trips.
 */
public record CustomerResponse(
        Long id,
        String name,
        String phone,
        String email,
        CustomerType customerType,
        int dueWindowDays,
        String telegramChatId,
        Instant createdAt,
        BigDecimal outstanding,
        BigDecimal paid,
        BigDecimal overdue
) {}
