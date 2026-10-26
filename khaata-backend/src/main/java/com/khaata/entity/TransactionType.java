package com.khaata.entity;

/**
 * The two kinds of ledger events.
 * <ul>
 *   <li>{@code CREDIT} — the customer took goods on credit (owes more).</li>
 *   <li>{@code REPAYMENT} — the customer paid money back (owes less).</li>
 * </ul>
 * The stored amount is always positive; this type supplies the sign.
 */
public enum TransactionType {
    CREDIT,
    REPAYMENT
}
