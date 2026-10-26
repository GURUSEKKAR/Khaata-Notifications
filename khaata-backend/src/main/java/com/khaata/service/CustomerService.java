package com.khaata.service;

import com.khaata.dto.CreateCustomerRequest;
import com.khaata.dto.CustomerResponse;
import com.khaata.dto.UpdateCustomerRequest;
import com.khaata.entity.Customer;
import com.khaata.entity.CustomerType;
import com.khaata.exception.BusinessRuleException;
import com.khaata.exception.NotFoundException;
import com.khaata.repository.CustomerRepository;
import com.khaata.repository.ReminderLogRepository;
import com.khaata.repository.TransactionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Customer management plus assembly of the customer-with-balances view used by
 * the right-hand dashboard list. Delegates all money math to {@link LedgerService}
 * so there is a single source of truth for balances.
 */
@Service
public class CustomerService {

    /** Default credit windows by customer type (days). */
    private static final int REGULAR_WINDOW = 30;
    private static final int NORMAL_WINDOW = 7;

    private final CustomerRepository customerRepository;
    private final LedgerService ledgerService;
    private final TransactionRepository transactionRepository;
    private final ReminderLogRepository reminderLogRepository;

    public CustomerService(CustomerRepository customerRepository,
                           LedgerService ledgerService,
                           TransactionRepository transactionRepository,
                           ReminderLogRepository reminderLogRepository) {
        this.customerRepository = customerRepository;
        this.ledgerService = ledgerService;
        this.transactionRepository = transactionRepository;
        this.reminderLogRepository = reminderLogRepository;
    }

    @Transactional
    public CustomerResponse create(CreateCustomerRequest req) {
        if (customerRepository.existsByPhone(req.phone())) {
            throw new BusinessRuleException("A customer with phone " + req.phone() + " already exists.");
        }
        int window = req.dueWindowDays() != null
                ? req.dueWindowDays()
                : defaultWindow(req.customerType());

        Customer customer = Customer.builder()
                .name(req.name().trim())
                .phone(req.phone().trim())
                .email(req.email() != null ? req.email().trim() : null)
                .customerType(req.customerType())
                .dueWindowDays(window)
                .telegramChatId(blankToNull(req.telegramChatId()))
                .createdAt(Instant.now())
                .version(0L)
                .build();

        return toResponse(customerRepository.save(customer));
    }

    @Transactional(readOnly = true)
    public CustomerResponse get(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Customer not found: " + id));
        return toResponse(customer);
    }

    /** Update a customer's editable details. Phone must stay unique. */
    @Transactional
    public CustomerResponse update(Long id, UpdateCustomerRequest req) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Customer not found: " + id));

        String newPhone = req.phone().trim();
        if (!newPhone.equals(customer.getPhone())
                && customerRepository.existsByPhone(newPhone)) {
            throw new BusinessRuleException("Another customer already uses phone " + newPhone + ".");
        }

        customer.setName(req.name().trim());
        customer.setPhone(newPhone);
        customer.setEmail(req.email() != null && !req.email().isBlank() ? req.email().trim() : null);
        customer.setCustomerType(req.customerType());
        customer.setDueWindowDays(req.dueWindowDays() != null
                ? req.dueWindowDays()
                : defaultWindow(req.customerType()));
        customer.setTelegramChatId(blankToNull(req.telegramChatId()));

        return toResponse(customerRepository.save(customer));
    }

    /**
     * Delete a customer and all their ledger/reminder history.
     *
     * <p>Business rule: a customer who still owes money cannot be deleted — you must
     * not accidentally erase an outstanding debt. The caller (controller) is
     * responsible for having verified the owner's password before invoking this.
     */
    @Transactional
    public void delete(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Customer not found: " + id));

        BigDecimal outstanding = ledgerService.outstanding(id);
        if (outstanding.compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessRuleException(
                    "Cannot delete %s — they still owe ₹%s. Clear the balance first."
                            .formatted(customer.getName(), outstanding.toPlainString()));
        }

        // Remove dependent rows first (no FK cascade configured), then the customer.
        reminderLogRepository.deleteByCustomerId(id);
        transactionRepository.deleteByCustomerId(id);
        customerRepository.delete(customer);
    }

    /**
     * All customers with their derived balances, sorted by who owes the most
     * (most important to chase first) — handy for the dashboard list.
     */
    @Transactional(readOnly = true)
    public List<CustomerResponse> listWithBalances() {
        return customerRepository.findAll().stream()
                .map(this::toResponse)
                .sorted(Comparator.comparing(CustomerResponse::outstanding).reversed())
                .toList();
    }

    private int defaultWindow(CustomerType type) {
        return type == CustomerType.REGULAR ? REGULAR_WINDOW : NORMAL_WINDOW;
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private CustomerResponse toResponse(Customer c) {
        BigDecimal outstanding = ledgerService.outstanding(c.getId());
        BigDecimal paid = ledgerService.paid(c.getId());
        BigDecimal overdue = ledgerService.overdue(c.getId());
        return new CustomerResponse(
                c.getId(), c.getName(), c.getPhone(), c.getEmail(),
                c.getCustomerType(), c.getDueWindowDays(), c.getTelegramChatId(),
                c.getCreatedAt(), outstanding, paid, overdue);
    }
}
