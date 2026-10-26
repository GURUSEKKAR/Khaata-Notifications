package com.khaata.service.notification;

import com.khaata.entity.ReminderChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Default dev sender: logs the message instead of calling an external API.
 *
 * <p>Activated when {@code khaata.notification.channel=console} (the default).
 * This lets the entire reminder flow run end-to-end with zero external accounts —
 * critical for building/demoing before WhatsApp credentials exist.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "khaata.notification.channel", havingValue = "console", matchIfMissing = true)
public class ConsoleSender implements NotificationSender {

    @Override
    public SendResult send(Recipient recipient, String message) {
        String to = recipient.phone() != null ? recipient.phone() : recipient.telegramChatId();
        log.info("[CONSOLE REMINDER] -> {} ({}): {}", recipient.name(), to, message);
        return SendResult.ok("logged to console");
    }

    @Override
    public ReminderChannel channel() {
        return ReminderChannel.CONSOLE;
    }
}
