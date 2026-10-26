package com.khaata.service.notification;

import com.khaata.entity.ReminderChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Sends reminders as email via SMTP (Gmail). Activated when
 * {@code khaata.notification.channel=email}. Implements the same
 * {@link NotificationSender} interface as the other channels, so the reminder
 * pipeline (producer → queue → consumer) is unchanged — only the wired bean differs.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "khaata.notification.channel", havingValue = "email")
public class EmailSender implements NotificationSender {

    private final JavaMailSender mailSender;
    private final String from;

    public EmailSender(JavaMailSender mailSender,
                       @Value("${khaata.mail.from:${spring.mail.username:}}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    @Override
    public SendResult send(Recipient recipient, String message) {
        if (recipient.email() == null || recipient.email().isBlank()) {
            return SendResult.fail("no email address on file for " + recipient.name());
        }
        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            if (from != null && !from.isBlank()) {
                mail.setFrom(from);
            }
            mail.setTo(recipient.email());
            mail.setSubject("Payment reminder from your shop");
            mail.setText(message);
            mailSender.send(mail);
            log.info("Email reminder sent to {}", recipient.email());
            return SendResult.ok("email sent to " + recipient.email());
        } catch (Exception e) {
            log.warn("Email delivery failed for {}: {}", recipient.email(), e.getMessage());
            return SendResult.fail("email error: " + e.getMessage());
        }
    }

    @Override
    public ReminderChannel channel() {
        return ReminderChannel.EMAIL;
    }
}
