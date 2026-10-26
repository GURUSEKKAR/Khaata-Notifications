package com.khaata.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * PRODUCER side of the queue. Anything that wants a reminder sent publishes a message
 * here instead of sending directly — so the caller returns immediately and the slow
 * delivery happens later in a background consumer.
 */
@Slf4j
@Component
public class ReminderPublisher {

    private final RabbitTemplate rabbitTemplate;

    public ReminderPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /** Enqueue a reminder for one customer. Returns as soon as the message is accepted. */
    public void publish(Long customerId, String reason) {
        try {
            ReminderMessage msg = ReminderMessage.of(customerId, reason);
            rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.ROUTING_KEY, msg);
            log.info("Published reminder message for customer {} (reason: {})", customerId, reason);
        } catch (Exception e) {
            // Never let a broker outage break the user action that triggered this.
            // The scheduled sweep will re-enqueue overdue customers later.
            log.warn("Could not publish reminder for customer {} ({}): {}",
                    customerId, reason, e.getMessage());
        }
    }
}
