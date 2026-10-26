package com.khaata.service;

import com.khaata.dto.DashboardSummaryResponse;
import com.khaata.entity.Customer;
import com.khaata.repository.CustomerRepository;
import com.khaata.repository.TransactionRepository;
import com.khaata.security.CurrentUser;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Builds the dashboard summary and finds reminder targets — all scoped to the
 * current owner, so each shopkeeper sees only their own totals.
 */
@Service
public class DashboardService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");

    private final TransactionRepository transactionRepository;
    private final CustomerRepository customerRepository;
    private final CurrentUser currentUser;

    public DashboardService(TransactionRepository transactionRepository,
                            CustomerRepository customerRepository,
                            CurrentUser currentUser) {
        this.transactionRepository = transactionRepository;
        this.customerRepository = customerRepository;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse summary() {
        LocalDate today = LocalDate.now(ZONE);
        List<Customer> owned = customerRepository.findByOwnerId(currentUser.id());

        BigDecimal totalOutstanding = BigDecimal.ZERO;
        BigDecimal totalPaid = BigDecimal.ZERO;
        BigDecimal overdue = BigDecimal.ZERO;

        for (Customer c : owned) {
            BigDecimal outstanding = transactionRepository.outstandingForCustomer(c.getId());
            BigDecimal paid = transactionRepository.paidForCustomer(c.getId());
            BigDecimal overdueCredit = transactionRepository.overdueCreditForCustomer(c.getId(), today);
            totalOutstanding = totalOutstanding.add(outstanding);
            totalPaid = totalPaid.add(paid);
            overdue = overdue.add(overdueCredit.min(outstanding).max(BigDecimal.ZERO));
        }

        return new DashboardSummaryResponse(
                totalOutstanding, totalPaid, totalOutstanding, overdue, owned.size());
    }

    /** The current owner's customers who are overdue — targets for a manual run. */
    @Transactional(readOnly = true)
    public List<Customer> customersNeedingReminder() {
        return overdueAmong(customerRepository.findByOwnerId(currentUser.id()));
    }

    /** Every overdue customer across all owners — used by the scheduled job. */
    @Transactional(readOnly = true)
    public List<Customer> allCustomersNeedingReminder() {
        return overdueAmong(customerRepository.findAll());
    }

    private List<Customer> overdueAmong(List<Customer> customers) {
        LocalDate today = LocalDate.now(ZONE);
        return customers.stream()
                .filter(c -> {
                    BigDecimal overdueCredit = transactionRepository.overdueCreditForCustomer(c.getId(), today);
                    BigDecimal outstanding = transactionRepository.outstandingForCustomer(c.getId());
                    return overdueCredit.min(outstanding).compareTo(BigDecimal.ZERO) > 0;
                })
                .toList();
    }
}
