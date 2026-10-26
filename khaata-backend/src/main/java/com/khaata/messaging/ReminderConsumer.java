package com.khaata.messaging;

import com.khaata.entity.Customer;
import com.khaata.repository.CustomerRepository;
import com.khaata.service.ReminderProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * CONSUMER side of the queue — the background worker. A {@code @RabbitListener} pulls
 * messages off {@code khaata.reminders.queue} one at a time and performs the slow work
 * (composing + sending the reminder) via {@link ReminderProcessor}.
 *
 * <p>This is where the real decoupling pays off: the producer (API / scheduler) is
 * never blocked by delivery. If this worker throws, Spring AMQP retries per the
 * configured policy; once retries are exhausted the message is dead-lettered to
 * {@code khaata.reminders.dlq} instead of being lost.
 */
@Slf4j
@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
        name = "khaata.queue.enabled", havingValue = "true", matchIfMissing = true)
public class ReminderConsumer {

    private final CustomerRepository customerRepository;
    private final ReminderProcessor reminderProcessor;

    public ReminderConsumer(CustomerRepository customerRepository,
                            ReminderProcessor reminderProcessor) {
        this.customerRepository = customerRepository;
        this.reminderProcessor = reminderProcessor;
        log.info("ReminderConsumer initialized — listening on queue '{}'", RabbitConfig.QUEUE);
    }

    @RabbitListener(queues = RabbitConfig.QUEUE)
    public void onMessage(ReminderMessage message) {
        log.info("Consuming reminder message for customer {} (reason: {})",
                message.customerId(), message.reason());

        Customer customer = customerRepository.findById(message.customerId()).orElse(null);
        if (customer == null) {
            // Customer was deleted after the message was queued — nothing to do.
            log.warn("Customer {} no longer exists; discarding message.", message.customerId());
            return;
        }

        // force=true: the message being on the queue already means we decided to send.
        boolean sent = reminderProcessor.remindOne(customer, true);
        if (!sent) {
            // Throwing triggers Spring AMQP retry/backoff, then dead-letter on exhaustion.
            throw new ReminderDeliveryException(
                    "Delivery failed for customer " + customer.getId() + "; will retry / dead-letter.");
        }
    }

    /** Signals a failed delivery so the broker can retry and ultimately dead-letter. */
    static class ReminderDeliveryException extends RuntimeException {
        ReminderDeliveryException(String message) {
            super(message);
        }
    }
}
