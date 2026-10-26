package com.khaata.service.notification;

import com.khaata.config.WhatsAppProperties;
import com.khaata.entity.ReminderChannel;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Production sender: posts a template message to the WhatsApp Cloud API.
 *
 * <p>Activated when {@code khaata.notification.channel=whatsapp}. Implements the
 * SAME {@link NotificationSender} interface as {@link ConsoleSender}, so the
 * reminder engine is unchanged — only the wired bean differs (Strategy pattern).
 *
 * <p>Wrapped with Resilience4j {@code @Retry} (exponential backoff) and
 * {@code @CircuitBreaker} so a flaky or down WhatsApp API degrades gracefully
 * instead of hammering the endpoint or crashing the reminder run.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "khaata.notification.channel", havingValue = "whatsapp")
public class WhatsAppCloudSender implements NotificationSender {

    private final WebClient webClient;
    private final WhatsAppProperties props;

    public WhatsAppCloudSender(WebClient.Builder builder, WhatsAppProperties props) {
        this.props = props;
        this.webClient = builder.baseUrl(props.apiUrl()).build();
    }

    @Override
    @Retry(name = "whatsapp")
    @CircuitBreaker(name = "whatsapp", fallbackMethod = "fallback")
    public SendResult send(Recipient recipient, String message) {
        String phone = recipient.phone();
        // WhatsApp requires a pre-approved template for business-initiated messages.
        // We send the template with the reminder text as its body parameter.
        Map<String, Object> body = Map.of(
                "messaging_product", "whatsapp",
                "to", normalize(phone),
                "type", "template",
                "template", Map.of(
                        "name", props.templateName(),
                        "language", Map.of("code", props.templateLang()),
                        "components", new Object[]{
                                Map.of("type", "body", "parameters", new Object[]{
                                        Map.of("type", "text", "text", message)
                                })
                        }
                )
        );

        String response = webClient.post()
                .uri("/{phoneNumberId}/messages", props.phoneNumberId())
                .header("Authorization", "Bearer " + props.accessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        log.info("WhatsApp sent to {} -> {}", phone, response);
        return SendResult.ok("whatsapp accepted");
    }

    /** Resilience4j fallback when retries are exhausted or the breaker is open. */
    @SuppressWarnings("unused")
    private SendResult fallback(Recipient recipient, String message, Throwable t) {
        log.warn("WhatsApp delivery failed for {}: {}", recipient.phone(), t.getMessage());
        return SendResult.fail("whatsapp unavailable: " + t.getMessage());
    }

    /** Strip a leading '+' — the Cloud API expects digits only. */
    private String normalize(String phone) {
        return phone.startsWith("+") ? phone.substring(1) : phone;
    }

    @Override
    public ReminderChannel channel() {
        return ReminderChannel.WHATSAPP;
    }
}
