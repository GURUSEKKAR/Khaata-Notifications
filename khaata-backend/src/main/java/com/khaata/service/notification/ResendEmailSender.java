package com.khaata.service.notification;

import com.khaata.config.ResendProperties;
import com.khaata.entity.ReminderChannel;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * Sends reminder emails via the Resend HTTP API (https://api.resend.com/emails).
 *
 * <p>Activated when {@code khaata.notification.channel=resend}. Uses HTTPS (port 443)
 * instead of SMTP, so it works on cloud hosts that block SMTP ports (like Render's
 * free tier). Implements the same {@link NotificationSender} interface, so the
 * reminder pipeline is unchanged.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "khaata.notification.channel", havingValue = "resend")
public class ResendEmailSender implements NotificationSender {

    private final WebClient webClient;
    private final ResendProperties props;

    public ResendEmailSender(WebClient.Builder builder, ResendProperties props) {
        this.props = props;
        this.webClient = builder.baseUrl("https://api.resend.com").build();
    }

    @Override
    public SendResult send(Recipient recipient, String message) {
        if (recipient.email() == null || recipient.email().isBlank()) {
            return SendResult.fail("no email address on file for " + recipient.name());
        }
        String shop = recipient.shopName() != null && !recipient.shopName().isBlank()
                ? recipient.shopName() : "your shop";

        Map<String, Object> body = Map.of(
                "from", props.from(),
                "to", new String[]{recipient.email()},
                "subject", "Payment reminder from " + shop,
                "text", message);
        try {
            String response = webClient.post()
                    .uri("/emails")
                    .header("Authorization", "Bearer " + props.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            log.info("Resend email sent to {} -> {}", recipient.email(), response);
            return SendResult.ok("resend accepted");
        } catch (WebClientResponseException e) {
            String detail = "HTTP " + e.getRawStatusCode() + ": " + e.getResponseBodyAsString();
            log.warn("Resend delivery failed for {}: {}", recipient.email(), detail);
            return SendResult.fail("resend: " + detail);
        } catch (Exception e) {
            log.warn("Resend delivery failed for {}: {}", recipient.email(), e.getMessage());
            return SendResult.fail("resend error: " + e.getMessage());
        }
    }

    @Override
    public ReminderChannel channel() {
        return ReminderChannel.EMAIL;
    }
}
