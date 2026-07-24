package com.vantage.core.messaging.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class RabbitMQConfig {

    public static final String EXCHANGE = "vantage.events";
    public static final String QUEUE = "vantage.order.events";
    public static final String ROUTING_KEY = "OrderCreatedEvent";
    public static final String INVENTORY_QUEUE = "vantage.inventory.events";
    public static final String INVENTORY_RESERVED_ROUTING_KEY = "InventoryReservedEvent";
    public static final String INVENTORY_FAILED_ROUTING_KEY = "InventoryReservationFailedEvent";

    // Webhook delivery exchange and queue with DLX
    public static final String WEBHOOK_DELIVERY_EXCHANGE = "vantage.webhook.delivery.exchange";
    public static final String WEBHOOK_DELIVERY_QUEUE = "vantage.webhook.delivery.queue";
    public static final String WEBHOOK_DELIVERY_ROUTING_KEY = "webhook.delivery";
    public static final String WEBHOOK_DLX = "vantage.webhook.dlx";

    @Bean
    public DirectExchange vantageEventsExchange() {
        return new DirectExchange(EXCHANGE);
    }

    @Bean
    public Queue orderEventsQueue() {
        return QueueBuilder.durable(QUEUE).build();
    }

    @Bean
    public Binding orderEventsBinding(DirectExchange vantageEventsExchange, Queue orderEventsQueue) {
        return BindingBuilder.bind(orderEventsQueue).to(vantageEventsExchange()).with(ROUTING_KEY);
    }

    @Bean
    public Queue inventoryEventsQueue() {
        return QueueBuilder.durable(INVENTORY_QUEUE).build();
    }

    @Bean
    public Binding inventoryReservedBinding(DirectExchange vantageEventsExchange, Queue inventoryEventsQueue) {
        return BindingBuilder.bind(inventoryEventsQueue).to(vantageEventsExchange()).with(INVENTORY_RESERVED_ROUTING_KEY);
    }

    @Bean
    public Binding inventoryFailedBinding(DirectExchange vantageEventsExchange, Queue inventoryEventsQueue) {
        return BindingBuilder.bind(inventoryEventsQueue).to(vantageEventsExchange()).with(INVENTORY_FAILED_ROUTING_KEY);
    }

    @Bean
    public DirectExchange webhookDeliveryExchange() {
        return new DirectExchange(WEBHOOK_DELIVERY_EXCHANGE);
    }

    @Bean
    public Queue webhookDeliveryQueue() {
        return QueueBuilder.durable(WEBHOOK_DELIVERY_QUEUE)
                .withArgument("x-dead-letter-exchange", WEBHOOK_DLX)
                .withArgument("x-dead-letter-routing-key", "webhook.dlq")
                .build();
    }

    @Bean
    public Queue webhookDlq() {
        return QueueBuilder.durable("vantage.webhook.dlq").build();
    }

    @Bean
    public DirectExchange webhookDlxExchange() {
        return new DirectExchange(WEBHOOK_DLX);
    }

    @Bean
    public Binding webhookDeliveryBinding(DirectExchange webhookDeliveryExchange, Queue webhookDeliveryQueue) {
        return BindingBuilder.bind(webhookDeliveryQueue).to(webhookDeliveryExchange()).with(WEBHOOK_DELIVERY_ROUTING_KEY);
    }

    @Bean
    public Binding webhookDlxBinding(DirectExchange webhookDlxExchange, Queue webhookDlq) {
        return BindingBuilder.bind(webhookDlq).to(webhookDlxExchange()).with("webhook.dlq");
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setDefaultRequeueRejected(false);

        RetryTemplate retryTemplate = new RetryTemplate();
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(1000);
        backOffPolicy.setMultiplier(3.0);
        backOffPolicy.setMaxInterval(60000);
        retryTemplate.setBackOffPolicy(backOffPolicy);
        factory.setRetryTemplate(retryTemplate);
        return factory;
    }
}
