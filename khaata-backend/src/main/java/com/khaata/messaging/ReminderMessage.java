package com.khaata.messaging;

import java.io.Serializable;

/**
 * The payload published to the reminder queue. Deliberately small — it carries only
 * the customer id and why the reminder was triggered. The consumer re-loads fresh data
 * from the database when it processes the message (so the message can't go stale).
 */
public record ReminderMessage(Long customerId, String reason) implements Serializable {
    public static ReminderMessage of(Long customerId, String reason) {
        return new ReminderMessage(customerId, reason);
    }
}
