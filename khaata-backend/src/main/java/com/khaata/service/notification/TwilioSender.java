package com.khaata.service.notification;

import com.khaata.config.TwilioProperties;
import com.khaata.entity.ReminderChannel;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * Sends reminders via Twilio — SMS or WhatsApp, chosen by {@code twilio.mode}.
 *
 * <p>Activated when {@code khaata.notification.channel=twilio}. Implements the same
 * {@link NotificationSender} interface as the other senders, so the reminder engine
 * is unchanged (Strategy pattern). Twilio's Messages API is a single form-encoded
 * POST with HTTP Basic auth (Account SID + Auth Token), so no SDK is required.
 *
 * <p>Wrapped with Resilience4j retry + circuit breaker so a flaky Twilio call
 * degrades gracefully instead of crashing the reminder run.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "khaata.notification.channel", havingValue = "twilio")
public class TwilioSender implements NotificationSender {

    private final WebClient webClient;
    private final TwilioProperties props;
    private final boolean whatsappMode;

    public TwilioSender(WebClient.Builder builder, TwilioProperties props) {
        this.props = props;
        this.whatsappMode = "whatsapp".equalsIgnoreCase(props.mode());
        // Basic auth: username = Account SID, password = Auth Token.
        this.webClient = builder
                .baseUrl("https://api.twilio.com")
                .defaultHeaders(h -> h.setBasicAuth(props.accountSid(), props.authToken()))
                .build();
    }

    @Override
    @Retry(name = "whatsapp")
    @CircuitBreaker(name = "whatsapp", fallbackMethod = "fallback")
    public SendResult send(Recipient recipient, String message) {
        String phone = recipient.phone();
        String from = whatsappMode ? props.whatsappFrom() : props.smsFrom();
        String to = whatsappMode ? "whatsapp:" + ensurePlus(phone) : ensurePlus(phone);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("From", from);
        form.add("To", to);
        form.add("Body", message);

        try {
            String response = webClient.post()
                    .uri("/2010-04-01/Accounts/{sid}/Messages.json", props.accountSid())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .bodyValue(form)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("Twilio {} sent from {} to {} -> {}",
                    whatsappMode ? "WhatsApp" : "SMS", from, to, response);
            return SendResult.ok("twilio accepted");
        } catch (WebClientResponseException e) {
            // Surface Twilio's actual error body (contains the real reason + code).
            log.error("Twilio rejected send from {} to {} : HTTP {} body={}",
                    from, to, e.getRawStatusCode(), e.getResponseBodyAsString());
            throw e; // let @Retry/@CircuitBreaker see it; fallback returns the result
        }
    }

    /** Resilience4j fallback when retries are exhausted or the breaker is open. */
    @SuppressWarnings("unused")
    private SendResult fallback(Recipient recipient, String message, Throwable t) {
        String detail = t instanceof WebClientResponseException w
                ? "HTTP " + w.getRawStatusCode() + ": " + w.getResponseBodyAsString()
                : t.getMessage();
        log.warn("Twilio delivery failed for {}: {}", recipient.phone(), detail);
        return SendResult.fail("twilio: " + detail);
    }

    /** Twilio wants E.164 (+countrycode...). Add the '+' if the caller omitted it. */
    private String ensurePlus(String phone) {
        return phone.startsWith("+") ? phone : "+" + phone;
    }

    @Override
    public ReminderChannel channel() {
        return whatsappMode ? ReminderChannel.WHATSAPP : ReminderChannel.SMS;
    }
}
