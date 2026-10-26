package com.khaata.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One immutable event in the append-only ledger.
 *
 * <p>This is the "event sourcing" heart of Khaata. A row is either a CREDIT
 * (customer owes more) or a REPAYMENT (owes less). Rows are only ever inserted —
 * never updated or deleted — so the full history is auditable and the current
 * balance is always re-computable as SUM(CREDIT) - SUM(REPAYMENT).
 *
 * <p>{@code amount} is always stored positive; {@link #type} supplies the sign.
 * Using {@link BigDecimal} (never double) avoids floating-point money errors.
 */
@Entity
@Table(name = "ledger_transaction", indexes = {
        @Index(name = "idx_txn_customer", columnList = "customer_id"),
        @Index(name = "idx_txn_due_date", columnList = "due_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LedgerTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TransactionType type;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(length = 255)
    private String note;

    /** Due date for CREDIT rows; null for REPAYMENT rows. Drives "overdue". */
    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
