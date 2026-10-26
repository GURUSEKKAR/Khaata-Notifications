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
import java.time.Instant;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Audit record of a single reminder attempt.
 *
 * <p>Serves two system-design purposes:
 * <ol>
 *   <li><b>Idempotency</b>: before sending, the reminder job checks whether a row
 *       already exists for (customer, today) so a re-run never double-messages.</li>
 *   <li><b>Observability / audit</b>: every SENT/FAILED/SKIPPED outcome is recorded,
 *       so the owner can see exactly who was chased and when.</li>
 * </ol>
 */
@Entity
@Table(name = "reminder_log", indexes = {
        @Index(name = "idx_reminder_customer_day", columnList = "customer_id,sent_on")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReminderLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ReminderChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ReminderStatus status;

    @Column(length = 500)
    private String message;

    /** Delivery detail — e.g. provider response or failure reason. */
    @Column(length = 500)
    private String detail;

    @Column(name = "sent_on", nullable = false)
    private LocalDate sentOn;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
