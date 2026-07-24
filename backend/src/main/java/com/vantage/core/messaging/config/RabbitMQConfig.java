// backend/src/main/java/com/vantage/core/messaging/config/RabbitMQConfig.java
package com.vantage.core.messaging.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
}
