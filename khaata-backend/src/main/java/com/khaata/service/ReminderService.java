package com.khaata.service;

import com.khaata.config.ReminderProperties;
import com.khaata.entity.Customer;
import com.khaata.exception.NotFoundException;
import com.khaata.repository.CustomerRepository;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * The automatic "chaser". Decoupled from the write path: recording a credit never
 * triggers a message inline — instead this scheduled job periodically finds overdue
 * customers and nudges them.
 *
 * <p>Key properties you can defend:
 * <ul>
 *   <li><b>Decoupling</b>: slow/flaky messaging never blocks the owner's API calls.</li>
 *   <li><b>Idempotency</b>: a reminder-log row per (customer, day) means a re-run or
 *       crash-retry never double-messages the same customer in a day.</li>
 *   <li><b>Pluggable channel</b>: it calls the notification interface; console in dev,
 *       WhatsApp in prod, decided purely by configuration.</li>
 * </ul>
 *
 * <p>The per-customer transactional work lives in {@link ReminderProcessor} so each
 * reminder commits independently (and so {@code @Transactional} proxying applies).
 */
@Slf4j
@Service
public class ReminderService {

    private final DashboardService dashboardService;
    private final ReminderProperties properties;
    private final CustomerRepository customerRepository;
    private final com.khaata.repository.ReminderLogRepository reminderLogRepository;
    private final com.khaata.security.CurrentUser currentUser;
    private final com.khaata.messaging.ReminderPublisher publisher;

    public ReminderService(DashboardService dashboardService,
                           ReminderProperties properties,
                           CustomerRepository customerRepository,
                           com.khaata.repository.ReminderLogRepository reminderLogRepository,
                           com.khaata.security.CurrentUser currentUser,
                           com.khaata.messaging.ReminderPublisher publisher) {
        this.dashboardService = dashboardService;
        this.properties = properties;
        this.customerRepository = customerRepository;
        this.reminderLogRepository = reminderLogRepository;
        this.currentUser = currentUser;
        this.publisher = publisher;
    }

    /**
     * Scheduled producer. Runs for EVERY owner's overdue customers (no user context)
     * and PUBLISHES a message per customer to the queue — the background consumer sends.
     */
    @Scheduled(cron = "${khaata.reminders.cron}")
    public void scheduledRun() {
        if (!properties.enabled()) {
            log.debug("Reminders disabled; skipping scheduled run.");
            return;
        }
        int queued = 0;
        for (Customer c : dashboardService.allCustomersNeedingReminder()) {
            publisher.publish(c.getId(), "scheduled-sweep");
            queued++;
        }
        log.info("Scheduled reminder run: {} messages queued.", queued);
    }

    /** Manual "Send Reminders Now" — queues the current owner's overdue customers. */
    public int runOnce() {
        List<Customer> targets = dashboardService.customersNeedingReminder();
        for (Customer c : targets) {
            publisher.publish(c.getId(), "manual-run");
        }
        return targets.size();
    }

    /** Queue a reminder for one of the current owner's customers (the per-row bell). */
    public boolean remindCustomerNow(Long customerId) {
        Customer customer = customerRepository.findByIdAndOwnerId(customerId, currentUser.id())
                .orElseThrow(() -> new NotFoundException("Customer not found: " + customerId));
        publisher.publish(customer.getId(), "manual-single");
        return true;
    }

    /** Reminder-log entries for only the current owner's customers. */
    public List<com.khaata.entity.ReminderLog> ownerLog() {
        java.util.Set<Long> myCustomerIds = customerRepository.findByOwnerId(currentUser.id())
                .stream().map(Customer::getId).collect(java.util.stream.Collectors.toSet());
        return reminderLogRepository.findTop100ByOrderByCreatedAtDesc().stream()
                .filter(r -> myCustomerIds.contains(r.getCustomerId()))
                .toList();
    }
}
