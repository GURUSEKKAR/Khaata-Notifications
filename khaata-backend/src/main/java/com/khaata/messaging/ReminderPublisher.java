package com.khaata.messaging;

import com.khaata.entity.Customer;
import com.khaata.repository.CustomerRepository;
import com.khaata.service.ReminderProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * PRODUCER side of the reminder pipeline, with a graceful fallback.
 *
 * <ul>
 *   <li><b>Queue enabled</b> ({@code khaata.queue.enabled=true}): publishes a message
 *       to RabbitMQ; the background {@link ReminderConsumer} sends it. Fully async.</li>
 *   <li><b>Queue disabled</b>: sends directly via {@link ReminderProcessor}, so the app
 *       still works with no broker (e.g. a free host without RabbitMQ).</li>
 * </ul>
 *
 * The {@link RabbitTemplate} is injected via {@link ObjectProvider} so this bean exists
 * even when RabbitMQ is not configured.
 */
@Slf4j
@Component
public class ReminderPublisher {

    private final boolean queueEnabled;
    private final ObjectProvider<RabbitTemplate> rabbitTemplate;
    private final ReminderProcessor reminderProcessor;
    private final CustomerRepository customerRepository;

    public ReminderPublisher(@Value("${khaata.queue.enabled:true}") boolean queueEnabled,
                             ObjectProvider<RabbitTemplate> rabbitTemplate,
                             @org.springframework.context.annotation.Lazy ReminderProcessor reminderProcessor,
                             CustomerRepository customerRepository) {
        this.queueEnabled = queueEnabled;
        this.rabbitTemplate = rabbitTemplate;
        this.reminderProcessor = reminderProcessor;
        this.customerRepository = customerRepository;
    }

    /** Enqueue (or, if the queue is off, directly send) a reminder for one customer. */
    public void publish(Long customerId, String reason) {
        if (queueEnabled) {
            RabbitTemplate template = rabbitTemplate.getIfAvailable();
            if (template != null) {
                try {
                    template.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.ROUTING_KEY,
                            ReminderMessage.of(customerId, reason));
                    log.info("Published reminder message for customer {} (reason: {})", customerId, reason);
                    return;
                } catch (Exception e) {
                    log.warn("Publish failed for customer {} ({}); falling back to direct send: {}",
                            customerId, reason, e.getMessage());
                }
            }
        }
        // Fallback: send directly (queue disabled or broker unavailable).
        sendDirect(customerId, reason);
    }

    private void sendDirect(Long customerId, String reason) {
        Customer c = customerRepository.findById(customerId).orElse(null);
        if (c == null) {
            log.warn("Customer {} not found; skipping direct send.", customerId);
            return;
        }
        reminderProcessor.remindOne(c, true);
        log.info("Sent reminder directly for customer {} (reason: {}, queue disabled)", customerId, reason);
    }
}
