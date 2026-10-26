package com.khaata.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A customer in the shop's khaata — the equivalent of one page in the paper book.
 *
 * <p>Note there is deliberately NO {@code balance} column. The balance is derived
 * from the append-only {@link LedgerTransaction} log. Storing a mutable balance
 * here would reintroduce the lost-update problem the ledger design avoids.
 *
 * <p>{@link Version} enables optimistic locking for any future field that does get
 * mutated concurrently (e.g. a cached balance), demonstrating concurrency control.
 */
@Entity
@Table(name = "customer")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The owner (app_user) this customer belongs to — enforces per-shop separation. */
    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(length = 160)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "customer_type", nullable = false, length = 16)
    private CustomerType customerType;

    @Column(name = "due_window_days", nullable = false)
    private int dueWindowDays;

    /** Optional Telegram chat id for reminders over Telegram. */
    @Column(name = "telegram_chat_id", length = 40)
    private String telegramChatId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Version
    @Column(nullable = false)
    private Long version;

    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean isRegular() {
        return customerType == CustomerType.REGULAR;
    }
}
