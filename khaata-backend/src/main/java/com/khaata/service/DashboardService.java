package com.khaata.service;

import com.khaata.dto.DashboardSummaryResponse;
import com.khaata.entity.Customer;
import com.khaata.repository.CustomerRepository;
import com.khaata.repository.TransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Builds the shop-wide summary for the four dashboard boxes.
 *
 * <p>The write model is the raw ledger; this is the read model that aggregates it
 * (a light-touch CQRS separation). For a shop's data volume these aggregate queries
 * are cheap; the documented scaling path is a cached/materialized balance if needed.
 */
@Service
public class DashboardService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");

    private final TransactionRepository transactionRepository;
    private final CustomerRepository customerRepository;

    public DashboardService(TransactionRepository transactionRepository,
                            CustomerRepository customerRepository) {
        this.transactionRepository = transactionRepository;
        this.customerRepository = customerRepository;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse summary() {
        BigDecimal totalOutstanding = transactionRepository.totalOutstanding();
        BigDecimal totalPaid = transactionRepository.totalPaid();

        // Overdue shop-wide: sum each customer's capped overdue amount.
        LocalDate today = LocalDate.now(ZONE);
        BigDecimal overdue = BigDecimal.ZERO;
        for (Customer c : customerRepository.findAll()) {
            BigDecimal overdueCredit =
                    transactionRepository.overdueCreditForCustomer(c.getId(), today);
            BigDecimal outstanding =
                    transactionRepository.outstandingForCustomer(c.getId());
            overdue = overdue.add(overdueCredit.min(outstanding).max(BigDecimal.ZERO));
        }

        long customerCount = customerRepository.count();

        return new DashboardSummaryResponse(
                totalOutstanding,
                totalPaid,
                totalOutstanding, // unpaidDebt == current outstanding
                overdue,
                customerCount);
    }

    /** Customers who currently owe money and are overdue — the reminder targets. */
    @Transactional(readOnly = true)
    public List<Customer> customersNeedingReminder() {
        LocalDate today = LocalDate.now(ZONE);
        return customerRepository.findAll().stream()
                .filter(c -> {
                    BigDecimal overdueCredit =
                            transactionRepository.overdueCreditForCustomer(c.getId(), today);
                    BigDecimal outstanding =
                            transactionRepository.outstandingForCustomer(c.getId());
                    BigDecimal overdue = overdueCredit.min(outstanding);
                    return overdue.compareTo(BigDecimal.ZERO) > 0;
                })
                .toList();
    }
}
