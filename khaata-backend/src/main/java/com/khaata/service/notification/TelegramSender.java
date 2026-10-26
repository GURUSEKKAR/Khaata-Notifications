package com.khaata.service.notification;

import com.khaata.config.TelegramProperties;
import com.khaata.entity.ReminderChannel;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * Sends reminders via a Telegram bot — free, no templates, instant.
 *
 * <p>Activated when {@code khaata.notification.channel=telegram}. Uses the Bot API's
 * {@code sendMessage} endpoint: a single JSON POST with the bot token in the URL.
 * The recipient is the customer's {@code telegramChatId}, falling back to a configured
 * default chat id (useful for demos). Implements the same {@link NotificationSender}
 * interface as the other channels, so the reminder engine is unchanged.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "khaata.notification.channel", havingValue = "telegram")
public class TelegramSender implements NotificationSender {

    private final WebClient webClient;
    private final TelegramProperties props;

    public TelegramSender(WebClient.Builder builder, TelegramProperties props) {
        this.props = props;
        this.webClient = builder.baseUrl("https://api.telegram.org").build();
    }

    @Override
    @Retry(name = "whatsapp")
    @CircuitBreaker(name = "whatsapp", fallbackMethod = "fallback")
    public SendResult send(Recipient recipient, String message) {
        String chatId = recipient.telegramChatId() != null && !recipient.telegramChatId().isBlank()
                ? recipient.telegramChatId()
                : props.defaultChatId();

        if (chatId == null || chatId.isBlank()) {
            return SendResult.fail("no telegram chat id for " + recipient.name()
                    + " and no telegram.default-chat-id configured");
        }

        Map<String, Object> body = Map.of(
                "chat_id", chatId,
                "text", message);

        try {
            String response = webClient.post()
                    .uri("/bot{token}/sendMessage", props.botToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            log.info("Telegram sent to chat {} -> {}", chatId, response);
            return SendResult.ok("telegram accepted");
        } catch (WebClientResponseException e) {
            log.error("Telegram rejected send to {} : HTTP {} body={}",
                    chatId, e.getRawStatusCode(), e.getResponseBodyAsString());
            throw e;
        }
    }

    @SuppressWarnings("unused")
    private SendResult fallback(Recipient recipient, String message, Throwable t) {
        String detail = t instanceof WebClientResponseException w
                ? "HTTP " + w.getRawStatusCode() + ": " + w.getResponseBodyAsString()
                : t.getMessage();
        log.warn("Telegram delivery failed for {}: {}", recipient.name(), detail);
        return SendResult.fail("telegram: " + detail);
    }

    @Override
    public ReminderChannel channel() {
        return ReminderChannel.TELEGRAM;
    }
}
