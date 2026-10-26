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
    private final ReminderProcessor reminderProcessor;
    private final ReminderProperties properties;
    private final CustomerRepository customerRepository;

    public ReminderService(DashboardService dashboardService,
                           ReminderProcessor reminderProcessor,
                           ReminderProperties properties,
                           CustomerRepository customerRepository) {
        this.dashboardService = dashboardService;
        this.reminderProcessor = reminderProcessor;
        this.properties = properties;
        this.customerRepository = customerRepository;
    }

    /** Scheduled entry point. Cron is configurable ({@code khaata.reminders.cron}). */
    @Scheduled(cron = "${khaata.reminders.cron}")
    public void scheduledRun() {
        if (!properties.enabled()) {
            log.debug("Reminders disabled; skipping scheduled run.");
            return;
        }
        int sent = runOnce();
        log.info("Reminder run complete. Messages sent: {}", sent);
    }

    /** Process all overdue customers once. Returns the number of reminders sent. */
    public int runOnce() {
        List<Customer> targets = dashboardService.customersNeedingReminder();
        int sent = 0;
        for (Customer c : targets) {
            if (reminderProcessor.remindOne(c)) {
                sent++;
            }
        }
        return sent;
    }

    /**
     * Manually send a reminder to one customer right now, regardless of whether they
     * are overdue or already reminded today. Powers the per-customer "Remind now" button.
     */
    public boolean remindCustomerNow(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new NotFoundException("Customer not found: " + customerId));
        return reminderProcessor.remindOne(customer, true);
    }
}
