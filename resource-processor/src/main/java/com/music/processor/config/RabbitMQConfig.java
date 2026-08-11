package com.music.processor.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "resource.exchange";
    public static final String QUEUE = "resource.uploaded.queue";
    public static final String ROUTING_KEY = "resource.uploaded";
    public static final String PROCESSED_QUEUE = "resource.processed.queue";
    public static final String PROCESSED_ROUTING_KEY = "resource.processed";

    @Bean
    DirectExchange resourceExchange() {
        return new DirectExchange(EXCHANGE);
    }

    @Bean
    Queue resourceUploadedQueue() {
        return QueueBuilder.durable(QUEUE).build();
    }

    @Bean
    Binding resourceUploadedBinding(Queue resourceUploadedQueue, DirectExchange resourceExchange) {
        return BindingBuilder.bind(resourceUploadedQueue).to(resourceExchange).with(ROUTING_KEY);
    }

    @Bean
    Queue resourceProcessedQueue() {
        return QueueBuilder.durable(PROCESSED_QUEUE).build();
    }

    @Bean
    Binding resourceProcessedBinding(Queue resourceProcessedQueue, DirectExchange resourceExchange) {
        return BindingBuilder.bind(resourceProcessedQueue).to(resourceExchange).with(PROCESSED_ROUTING_KEY);
    }

    @Bean
    MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
