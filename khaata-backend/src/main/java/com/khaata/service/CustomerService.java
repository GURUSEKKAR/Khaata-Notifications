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
import com.khaata.security.CurrentUser;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Customer management. Every operation is scoped to the currently authenticated
 * owner, so each shopkeeper only sees and manages their own customers.
 */
@Service
public class CustomerService {

    private static final int REGULAR_WINDOW = 30;
    private static final int NORMAL_WINDOW = 7;

    private final CustomerRepository customerRepository;
    private final LedgerService ledgerService;
    private final TransactionRepository transactionRepository;
    private final ReminderLogRepository reminderLogRepository;
    private final CurrentUser currentUser;

    public CustomerService(CustomerRepository customerRepository,
                           LedgerService ledgerService,
                           TransactionRepository transactionRepository,
                           ReminderLogRepository reminderLogRepository,
                           CurrentUser currentUser) {
        this.customerRepository = customerRepository;
        this.ledgerService = ledgerService;
        this.transactionRepository = transactionRepository;
        this.reminderLogRepository = reminderLogRepository;
        this.currentUser = currentUser;
    }

    @Transactional
    public CustomerResponse create(CreateCustomerRequest req) {
        Long ownerId = currentUser.id();
        if (customerRepository.existsByOwnerIdAndPhone(ownerId, req.phone().trim())) {
            throw new BusinessRuleException("A customer with phone " + req.phone() + " already exists.");
        }
        int window = req.dueWindowDays() != null ? req.dueWindowDays() : defaultWindow(req.customerType());

        Customer customer = Customer.builder()
                .ownerId(ownerId)
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
        return toResponse(requireOwned(id));
    }

    @Transactional
    public CustomerResponse update(Long id, UpdateCustomerRequest req) {
        Customer customer = requireOwned(id);
        String newPhone = req.phone().trim();
        if (!newPhone.equals(customer.getPhone())
                && customerRepository.existsByOwnerIdAndPhone(customer.getOwnerId(), newPhone)) {
            throw new BusinessRuleException("Another customer already uses phone " + newPhone + ".");
        }

        customer.setName(req.name().trim());
        customer.setPhone(newPhone);
        customer.setEmail(req.email() != null && !req.email().isBlank() ? req.email().trim() : null);
        customer.setCustomerType(req.customerType());
        customer.setDueWindowDays(req.dueWindowDays() != null ? req.dueWindowDays() : defaultWindow(req.customerType()));
        customer.setTelegramChatId(blankToNull(req.telegramChatId()));

        return toResponse(customerRepository.save(customer));
    }

    @Transactional
    public void delete(Long id) {
        Customer customer = requireOwned(id);
        BigDecimal outstanding = ledgerService.outstanding(id);
        if (outstanding.compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessRuleException(
                    "Cannot delete %s — they still owe ₹%s. Clear the balance first."
                            .formatted(customer.getName(), outstanding.toPlainString()));
        }
        reminderLogRepository.deleteByCustomerId(id);
        transactionRepository.deleteByCustomerId(id);
        customerRepository.delete(customer);
    }

    /** All of the current owner's customers, with balances, biggest debt first. */
    @Transactional(readOnly = true)
    public List<CustomerResponse> listWithBalances() {
        return customerRepository.findByOwnerId(currentUser.id()).stream()
                .map(this::toResponse)
                .sorted(Comparator.comparing(CustomerResponse::outstanding).reversed())
                .toList();
    }

    /**
     * Fetch a customer by id, but only if it belongs to the current owner.
     * A customer owned by someone else returns 404 — you can't see or touch it.
     */
    private Customer requireOwned(Long id) {
        return customerRepository.findByIdAndOwnerId(id, currentUser.id())
                .orElseThrow(() -> new NotFoundException("Customer not found: " + id));
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
