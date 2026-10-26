package com.khaata.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Type-safe binding of the {@code khaata.reminders.*} configuration keys. */
@ConfigurationProperties(prefix = "khaata.reminders")
public record ReminderProperties(
        boolean enabled,
        String cron
) {}
