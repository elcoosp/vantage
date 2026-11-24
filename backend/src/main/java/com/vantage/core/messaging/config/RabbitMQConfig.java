package com.vantage.core.messaging.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.DefaultClassMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import tools.jackson.databind.ObjectMapper;

@Configuration
@EnableScheduling
public class RabbitMQConfig {

    public static final String EXCHANGE = "vantage.events";
    public static final String QUEUE = "vantage.order.events";
    public static final String ROUTING_KEY = "OrderCreatedEvent";
    public static final String INVENTORY_QUEUE = "vantage.inventory.events";
    public static final String INVENTORY_RESERVED_ROUTING_KEY = "InventoryReservedEvent";
    public static final String INVENTORY_FAILED_ROUTING_KEY = "InventoryReservationFailedEvent";

    /**
     * JSON message converter for inbound listeners.
     *
     * <p>The outbox relay publishes payloads as raw JSON ({@code content-type: application/json})
     * without a Jackson {@code __TypeId__} header, so the listeners rely on the target type
     * declared by their {@code @RabbitListener} method signature. The {@link DefaultClassMapper}
     * is configured to trust the application packages so those payloads deserialize correctly.</p>
     */
    @Bean
    public MessageConverter jackson2JsonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        DefaultClassMapper classMapper = new DefaultClassMapper();
        classMapper.setTrustedPackages("com.vantage");
        converter.setClassMapper(classMapper);
        return converter;
    }

    /**
     * {@link ConnectionFactory} bean. Spring Boot 4 no longer auto-configures one from
     * {@code spring-boot-starter-amqp}, so it must be declared by hand.
     */
    @Bean
    public ConnectionFactory connectionFactory(@org.springframework.beans.factory.annotation.Value("${spring.rabbitmq.host:localhost}") String host,
                                               @org.springframework.beans.factory.annotation.Value("${spring.rabbitmq.port:5672}") int port,
                                               @org.springframework.beans.factory.annotation.Value("${spring.rabbitmq.username:guest}") String username,
                                               @org.springframework.beans.factory.annotation.Value("${spring.rabbitmq.password:guest}") String password) {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory(host, port);
        connectionFactory.setUsername(username);
        connectionFactory.setPassword(password);
        return connectionFactory;
    }

    /**
     * Explicit {@link RabbitTemplate} bean. Spring Boot 4 no longer auto-configures one from
     * {@code spring-boot-starter-amqp}, so it must be declared by hand.
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        return rabbitTemplate;
    }

    /**
     * {@link RabbitAdmin} is needed by {@code OutboxPoller} to declare queues/exchanges lazily
     * after the connection is established.
     */
    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }

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
        return factory;
    }
}