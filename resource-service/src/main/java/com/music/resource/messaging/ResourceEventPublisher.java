package com.music.resource.messaging;

import com.music.resource.config.RabbitMQConfig;
import com.music.resource.dto.ResourceUploadedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class ResourceEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(ResourceEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public ResourceEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishResourceUploaded(Long resourceId) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE,
                RabbitMQConfig.ROUTING_KEY,
                new ResourceUploadedEvent(resourceId));
        log.info(
                "Published ResourceUploadedEvent: resourceId={}, exchange={}, routingKey={}",
                resourceId,
                RabbitMQConfig.EXCHANGE,
                RabbitMQConfig.ROUTING_KEY);
    }
}
