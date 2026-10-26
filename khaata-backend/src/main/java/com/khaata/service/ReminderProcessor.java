package com.khaata.service;

import com.khaata.entity.Customer;
import com.khaata.entity.ReminderLog;
import com.khaata.entity.ReminderStatus;
import com.khaata.repository.ReminderLogRepository;
import com.khaata.service.notification.NotificationSender;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles a single customer's reminder in its own transaction.
 *
 * <p>Deliberately a separate bean from {@link ReminderService}: Spring's
 * {@code @Transactional} works via proxies, and a self-invoked method on the same
 * bean would bypass the proxy. Putting the per-customer transaction here guarantees
 * each reminder commits/rolls back independently (one failure never aborts the batch).
 */
@Slf4j
@Service
public class ReminderProcessor {

    private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");

    private final LedgerService ledgerService;
    private final ReminderLogRepository reminderLogRepository;
    private final NotificationSender notificationSender;

    public ReminderProcessor(LedgerService ledgerService,
                             ReminderLogRepository reminderLogRepository,
                             NotificationSender notificationSender) {
        this.ledgerService = ledgerService;
        this.reminderLogRepository = reminderLogRepository;
        this.notificationSender = notificationSender;
    }

    /** @return true if a reminder was actually sent. */
    @Transactional
    public boolean remindOne(Customer customer) {
        return remindOne(customer, false);
    }

    /**
     * Send a reminder to one customer. When {@code force} is true, the "already
     * reminded today" idempotency check is skipped — used by the manual
     * "remind now" action so demos/tests can fire on demand.
     *
     * @return true if a reminder was actually sent.
     */
    @Transactional
    public boolean remindOne(Customer customer, boolean force) {
        LocalDate today = LocalDate.now(ZONE);

        if (!force) {
            boolean alreadyHandled = reminderLogRepository
                    .existsByCustomerIdAndSentOnAndStatusIn(
                            customer.getId(), today,
                            List.of(ReminderStatus.SENT, ReminderStatus.SKIPPED));
            if (alreadyHandled) {
                log.debug("Already reminded {} today; skipping.", customer.getName());
                return false;
            }
        }

        BigDecimal overdue = ledgerService.overdue(customer.getId());
        String message = buildMessage(customer, overdue);

        NotificationSender.Recipient recipient = new NotificationSender.Recipient(
                customer.getName(), customer.getPhone(), customer.getTelegramChatId());
        NotificationSender.SendResult result = notificationSender.send(recipient, message);

        reminderLogRepository.save(ReminderLog.builder()
                .customerId(customer.getId())
                .channel(notificationSender.channel())
                .status(result.success() ? ReminderStatus.SENT : ReminderStatus.FAILED)
                .message(message)
                .detail(result.detail())
                .sentOn(today)
                .createdAt(Instant.now())
                .build());

        return result.success();
    }

    private String buildMessage(Customer customer, BigDecimal overdue) {
        return "Namaste %s, this is a friendly reminder from your shop. You have an overdue balance of Rs. %s. Please repay at your earliest convenience. Dhanyavaad."
                .formatted(customer.getName(), overdue.toPlainString());
    }
}
