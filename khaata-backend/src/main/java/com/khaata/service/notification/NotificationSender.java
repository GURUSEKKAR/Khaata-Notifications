package com.khaata.service.notification;

import com.khaata.entity.ReminderChannel;

/**
 * Strategy interface for delivering a reminder. The reminder engine depends only
 * on this abstraction, not on any concrete channel — this is Dependency Inversion
 * (the D in SOLID). Swapping console → WhatsApp → SMS → Telegram requires no change
 * to the reminder logic, only a different bean selected by configuration.
 */
public interface NotificationSender {

    /**
     * Deliver a message to a recipient. Implementations use whichever field they
     * need (phone for SMS/WhatsApp, telegramChatId for Telegram).
     *
     * @return a result describing success/failure; implementations must not throw
     *         for ordinary delivery failures (they return a FAILED result instead),
     *         so the engine can log and record gracefully.
     */
    SendResult send(Recipient recipient, String message);

    /** Which channel this implementation represents (for logging/audit). */
    ReminderChannel channel();

    /** Everything a sender might need to reach a customer. */
    record Recipient(String name, String phone, String telegramChatId) {
        public static Recipient ofPhone(String phone) {
            return new Recipient(null, phone, null);
        }
    }

    /** Outcome of a single send attempt. */
    record SendResult(boolean success, String detail) {
        public static SendResult ok(String detail) {
            return new SendResult(true, detail);
        }
        public static SendResult fail(String detail) {
            return new SendResult(false, detail);
        }
    }
}
