package com.khaata.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Type-safe binding of the {@code whatsapp.*} configuration keys. */
@ConfigurationProperties(prefix = "whatsapp")
public record WhatsAppProperties(
        String apiUrl,
        String phoneNumberId,
        String accessToken,
        String templateName,
        String templateLang
) {}
