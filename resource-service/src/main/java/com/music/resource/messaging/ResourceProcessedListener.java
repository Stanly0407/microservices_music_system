package com.music.resource.messaging;

import com.music.resource.config.RabbitMQConfig;
import com.music.resource.dto.ResourceProcessedEvent;
import com.music.resource.service.ResourceService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ResourceProcessedListener {

    private final ResourceService resourceService;

    public ResourceProcessedListener(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @RabbitListener(queues = RabbitMQConfig.PROCESSED_QUEUE)
    public void onResourceProcessed(ResourceProcessedEvent event) {
        resourceService.markProcessed(event.resourceId());
    }
}
