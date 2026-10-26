package com.khaata.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/** Wires shared beans and enables the type-safe configuration properties. */
@Configuration
@EnableConfigurationProperties({ReminderProperties.class, WhatsAppProperties.class, TwilioProperties.class, TelegramProperties.class, ResendProperties.class})
public class AppConfig {

    /** Shared WebClient builder used by the WhatsApp sender. */
    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}
