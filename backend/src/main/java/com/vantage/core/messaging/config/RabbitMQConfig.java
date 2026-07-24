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
    public static final String ROUTING_KEY = "order.created";

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
}
