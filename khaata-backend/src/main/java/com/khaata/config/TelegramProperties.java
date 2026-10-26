package com.khaata.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Type-safe binding of the {@code telegram.*} configuration keys.
 *
 * <p>{@link #botToken} comes from @BotFather. {@link #defaultChatId} is an optional
 * fallback: if a customer has no telegram chat id of their own, the reminder is sent
 * to this chat (handy for demos where everything goes to the owner's own chat).
 */
@ConfigurationProperties(prefix = "telegram")
public record TelegramProperties(
        String botToken,
        String defaultChatId
) {}
