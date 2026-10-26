package com.khaata.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declares the RabbitMQ topology for the reminder pipeline.
 *
 * <pre>
 *   producer ──► [ khaata.reminders.exchange ] ──(routing key)──► khaata.reminders.queue
 *                                                                        │ (on repeated failure)
 *                                                                        ▼
 *                           khaata.reminders.dlx ──► khaata.reminders.dlq (dead letters)
 * </pre>
 *
 * The main queue is configured so that a message which keeps failing is routed to a
 * dead-letter exchange/queue instead of being lost or retried forever. Messages are
 * JSON-serialized so the consumer receives a typed {@link ReminderMessage}.
 */
@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "khaata.reminders.exchange";
    public static final String QUEUE = "khaata.reminders.queue";
    public static final String ROUTING_KEY = "reminder.send";

    public static final String DLX = "khaata.reminders.dlx";
    public static final String DLQ = "khaata.reminders.dlq";
    public static final String DLQ_ROUTING_KEY = "reminder.dead";

    @Bean
    public DirectExchange reminderExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DLX, true, false);
    }

    @Bean
    public Queue reminderQueue() {
        return QueueBuilder.durable(QUEUE)
                .withArgument("x-dead-letter-exchange", DLX)
                .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DLQ).build();
    }

    @Bean
    public Binding reminderBinding() {
        return BindingBuilder.bind(reminderQueue()).to(reminderExchange()).with(ROUTING_KEY);
    }

    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchange()).with(DLQ_ROUTING_KEY);
    }

    /** JSON (de)serialization so producer and consumer exchange typed objects. */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory cf, MessageConverter converter) {
        RabbitTemplate t = new RabbitTemplate(cf);
        t.setMessageConverter(converter);
        return t;
    }
}
