package com.khaata.service;

import com.khaata.dto.CreateTransactionRequest;
import com.khaata.dto.LedgerEntryResponse;
import com.khaata.dto.TransactionResponse;
import com.khaata.entity.Customer;
import com.khaata.entity.LedgerTransaction;
import com.khaata.entity.TransactionType;
import com.khaata.exception.BusinessRuleException;
import com.khaata.exception.NotFoundException;
import com.khaata.repository.CustomerRepository;
import com.khaata.repository.TransactionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The heart of Khaata: the append-only ledger and all money math.
 *
 * <p>Design notes you can defend:
 * <ul>
 *   <li><b>Append-only</b>: {@link #record} only ever INSERTs a {@link LedgerTransaction}.
 *       Balances are derived by summing, never stored-and-overwritten. This gives a full
 *       audit trail and removes the classic read-modify-write lost-update race on a balance.</li>
 *   <li><b>ACID</b>: {@code @Transactional} makes the "check outstanding + insert" atomic,
 *       so a repayment can't slip past the overpayment guard under concurrency.</li>
 *   <li><b>BigDecimal</b>: money is never a floating-point type.</li>
 * </ul>
 */
@Service
public class LedgerService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");

    private final TransactionRepository transactionRepository;
    private final CustomerRepository customerRepository;
    private final com.khaata.security.CurrentUser currentUser;
    private final com.khaata.messaging.ReminderPublisher reminderPublisher;

    public LedgerService(TransactionRepository transactionRepository,
                         CustomerRepository customerRepository,
                         com.khaata.security.CurrentUser currentUser,
                         com.khaata.messaging.ReminderPublisher reminderPublisher) {
        this.transactionRepository = transactionRepository;
        this.customerRepository = customerRepository;
        this.currentUser = currentUser;
        this.reminderPublisher = reminderPublisher;
    }

    /**
     * Record a CREDIT or REPAYMENT for a customer the current owner owns.
     * For a REPAYMENT, v1 rejects any amount that exceeds the current outstanding
     * (no advance credit) — a deliberate, simple business rule.
     */
    @Transactional
    public TransactionResponse record(Long customerId, CreateTransactionRequest req) {
        Customer customer = customerRepository.findByIdAndOwnerId(customerId, currentUser.id())
                .orElseThrow(() -> new NotFoundException("Customer not found: " + customerId));

        if (req.type() == TransactionType.REPAYMENT) {
            BigDecimal outstanding = transactionRepository.outstandingForCustomer(customerId);
            if (req.amount().compareTo(outstanding) > 0) {
                throw new BusinessRuleException(
                        "Repayment (%s) exceeds outstanding balance (%s)."
                                .formatted(req.amount(), outstanding));
            }
        }

        LocalDate dueDate = null;
        if (req.type() == TransactionType.CREDIT) {
            dueDate = req.dueDate() != null
                    ? req.dueDate()
                    : LocalDate.now(ZONE).plusDays(customer.getDueWindowDays());
        }

        LedgerTransaction txn = LedgerTransaction.builder()
                .customerId(customerId)
                .type(req.type())
                .amount(req.amount())
                .note(req.note())
                .dueDate(dueDate)
                .createdAt(Instant.now())
                .build();

        LedgerTransaction saved = transactionRepository.save(txn);

        // Event-driven publish: if this credit is already overdue at creation
        // (e.g. a backdated entry), enqueue a reminder immediately. Normal future-dated
        // credit is picked up by the scheduled sweep once it becomes due.
        if (req.type() == TransactionType.CREDIT && dueDate != null
                && dueDate.isBefore(LocalDate.now(ZONE))) {
            reminderPublisher.publish(customerId, "credit-recorded-overdue");
        }

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> history(Long customerId) {
        if (customerRepository.findByIdAndOwnerId(customerId, currentUser.id()).isEmpty()) {
            throw new NotFoundException("Customer not found: " + customerId);
        }
        return transactionRepository.findByCustomerIdOrderByCreatedAtDesc(customerId)
                .stream().map(LedgerService::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public BigDecimal outstanding(Long customerId) {
        return transactionRepository.outstandingForCustomer(customerId);
    }

    /**
     * Full history for a customer, oldest-first, each row carrying the running
     * balance after it — exactly what the UI history panel renders. Computing the
     * running balance in chronological order makes the ledger read like a passbook.
     */
    @Transactional(readOnly = true)
    public List<LedgerEntryResponse> historyWithBalance(Long customerId) {
        if (customerRepository.findByIdAndOwnerId(customerId, currentUser.id()).isEmpty()) {
            throw new NotFoundException("Customer not found: " + customerId);
        }
        LocalDate today = LocalDate.now(ZONE);

        // Oldest-first so the running balance accumulates correctly.
        List<LedgerTransaction> txns =
                transactionRepository.findByCustomerIdOrderByCreatedAtDesc(customerId)
                        .stream()
                        .sorted(Comparator.comparing(LedgerTransaction::getCreatedAt))
                        .toList();

        List<LedgerEntryResponse> out = new ArrayList<>(txns.size());
        BigDecimal running = BigDecimal.ZERO;
        for (LedgerTransaction t : txns) {
            running = t.getType() == TransactionType.CREDIT
                    ? running.add(t.getAmount())
                    : running.subtract(t.getAmount());
            boolean overdue = t.getType() == TransactionType.CREDIT
                    && t.getDueDate() != null
                    && t.getDueDate().isBefore(today)
                    && running.compareTo(BigDecimal.ZERO) > 0;
            out.add(new LedgerEntryResponse(
                    t.getId(), t.getType(), t.getAmount(), running,
                    t.getNote(), t.getDueDate(), overdue, t.getCreatedAt()));
        }
        // Newest-first for display (most recent activity on top).
        Collections.reverse(out);
        return out;
    }

    @Transactional(readOnly = true)
    public BigDecimal paid(Long customerId) {
        return transactionRepository.paidForCustomer(customerId);
    }

    /**
     * Overdue for a customer = overdue credit, but capped at current outstanding
     * (you can't owe overdue more than you still owe in total).
     */
    @Transactional(readOnly = true)
    public BigDecimal overdue(Long customerId) {
        BigDecimal overdueCredit =
                transactionRepository.overdueCreditForCustomer(customerId, LocalDate.now(ZONE));
        BigDecimal outstanding = transactionRepository.outstandingForCustomer(customerId);
        return overdueCredit.min(outstanding).max(BigDecimal.ZERO);
    }

    static TransactionResponse toResponse(LedgerTransaction t) {
        return new TransactionResponse(
                t.getId(), t.getCustomerId(), t.getType(),
                t.getAmount(), t.getNote(), t.getDueDate(), t.getCreatedAt());
    }
}
