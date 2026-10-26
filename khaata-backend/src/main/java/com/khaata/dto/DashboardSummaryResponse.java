package com.khaata.dto;

import java.math.BigDecimal;

/**
 * The four dashboard boxes, shop-wide.
 * <ul>
 *   <li>{@code totalOutstanding} — current money still owed across all customers.</li>
 *   <li>{@code totalPaid} — total ever repaid.</li>
 *   <li>{@code unpaidDebt} — same as outstanding (kept explicit for the UI box).</li>
 *   <li>{@code overdue} — the portion of outstanding whose due date has passed.</li>
 * </ul>
 */
public record DashboardSummaryResponse(
        BigDecimal totalOutstanding,
        BigDecimal totalPaid,
        BigDecimal unpaidDebt,
        BigDecimal overdue,
        long customerCount
) {}
