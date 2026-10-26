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
    private final com.khaata.repository.AppUserRepository userRepository;

    public ReminderProcessor(LedgerService ledgerService,
                             ReminderLogRepository reminderLogRepository,
                             NotificationSender notificationSender,
                             com.khaata.repository.AppUserRepository userRepository) {
        this.ledgerService = ledgerService;
        this.reminderLogRepository = reminderLogRepository;
        this.notificationSender = notificationSender;
        this.userRepository = userRepository;
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
        com.khaata.entity.AppUser owner = userRepository.findById(customer.getOwnerId()).orElse(null);
        String shopName = owner != null ? owner.getShopName() : "our shop";
        String message = buildMessage(customer, overdue, owner);

        NotificationSender.Recipient recipient = new NotificationSender.Recipient(
                customer.getName(), customer.getPhone(), customer.getEmail(),
                customer.getTelegramChatId(), shopName);
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

    private String buildMessage(Customer customer, BigDecimal overdue, com.khaata.entity.AppUser owner) {
        String shopName = owner != null ? owner.getShopName() : "our shop";
        String ownerName = owner != null ? owner.getOwnerName() : "The owner";
        String upiId = owner != null ? owner.getUpiId() : null;

        StringBuilder sb = new StringBuilder();
        sb.append("Dear ").append(customer.getName()).append(",\n\n");
        sb.append("This is a respectful reminder from ").append(shopName).append(". ");
        sb.append("As per our records, you have a pending balance of Rs. ")
          .append(overdue.toPlainString()).append(" on your khaata with us.\n\n");
        sb.append("We kindly request you to clear it at your convenience.");
        if (upiId != null && !upiId.isBlank()) {
            sb.append(" You can pay quickly via UPI to:\n")
              .append("    UPI ID: ").append(upiId).append("\n");
        }
        sb.append("\nThank you for your continued trust.\n\n");
        sb.append("Warm regards,\n");
        sb.append(ownerName).append("\n");
        sb.append(shopName);
        return sb.toString();
    }
}
