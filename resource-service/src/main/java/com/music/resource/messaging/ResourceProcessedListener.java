package com.music.resource.messaging;

import com.music.resource.config.RabbitMQConfig;
import com.music.resource.dto.ResourceProcessedEvent;
import com.music.resource.service.ResourceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ResourceProcessedListener {

    private static final Logger log = LoggerFactory.getLogger(ResourceProcessedListener.class);

    private final ResourceService resourceService;

    public ResourceProcessedListener(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @RabbitListener(queues = RabbitMQConfig.PROCESSED_QUEUE)
    public void onResourceProcessed(ResourceProcessedEvent event) {
        log.info("Received ResourceProcessedEvent: resourceId={}, queue={}", event.resourceId(), RabbitMQConfig.PROCESSED_QUEUE);
        resourceService.markProcessed(event.resourceId());
    }
}
