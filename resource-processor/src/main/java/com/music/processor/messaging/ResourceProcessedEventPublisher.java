package com.music.processor.messaging;

import com.music.processor.config.RabbitMQConfig;
import com.music.processor.dto.ResourceProcessedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class ResourceProcessedEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(ResourceProcessedEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public ResourceProcessedEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishResourceProcessed(Long resourceId) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE,
                RabbitMQConfig.PROCESSED_ROUTING_KEY,
                new ResourceProcessedEvent(resourceId));
        log.info(
                "Published ResourceProcessedEvent: resourceId={}, exchange={}, routingKey={}",
                resourceId,
                RabbitMQConfig.EXCHANGE,
                RabbitMQConfig.PROCESSED_ROUTING_KEY);
    }
}
