package com.khaata.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Type-safe binding of the {@code twilio.*} configuration keys.
 *
 * <p>{@code mode} selects which channel this sender uses:
 * <ul>
 *   <li>{@code sms} — sends from {@link #smsFrom} (a Twilio phone number).</li>
 *   <li>{@code whatsapp} — sends from {@link #whatsappFrom} (the sandbox/number,
 *       e.g. {@code whatsapp:+14155238886}); recipient numbers are auto-prefixed
 *       with {@code whatsapp:}.</li>
 * </ul>
 */
@ConfigurationProperties(prefix = "twilio")
public record TwilioProperties(
        String accountSid,
        String authToken,
        String mode,          // "sms" or "whatsapp"
        String smsFrom,       // e.g. +14150000000
        String whatsappFrom   // e.g. whatsapp:+14155238886
) {}
