package com.music.processor.messaging;

import com.music.processor.client.ResourceServiceClient;
import com.music.processor.client.SongServiceClient;
import com.music.processor.config.RabbitMQConfig;
import com.music.processor.dto.ResourceUploadedEvent;
import com.music.processor.dto.SongMetadataPayload;
import com.music.processor.exception.Mp3ProcessingException;
import com.music.processor.service.Mp3MetadataExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

import java.util.Map;

@Component
public class ResourceUploadedListener {

    private static final Logger log = LoggerFactory.getLogger(ResourceUploadedListener.class);

    private final ResourceServiceClient resourceServiceClient;
    private final SongServiceClient songServiceClient;
    private final Mp3MetadataExtractor metadataExtractor;
    private final ResourceProcessedEventPublisher resourceProcessedEventPublisher;

    public ResourceUploadedListener(
            ResourceServiceClient resourceServiceClient,
            SongServiceClient songServiceClient,
            Mp3MetadataExtractor metadataExtractor,
            ResourceProcessedEventPublisher resourceProcessedEventPublisher) {
        this.resourceServiceClient = resourceServiceClient;
        this.songServiceClient = songServiceClient;
        this.metadataExtractor = metadataExtractor;
        this.resourceProcessedEventPublisher = resourceProcessedEventPublisher;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE)
    public void onResourceUploaded(ResourceUploadedEvent event) {
        long resourceId = event.resourceId();
        log.info("Processing resource: resourceId={}", resourceId);
        try {
            byte[] mp3Data = resourceServiceClient.getResourceData(resourceId);
            Map<String, String> tags = metadataExtractor.extractTags(mp3Data);
            SongMetadataPayload metadata = metadataExtractor.toSongMetadata(resourceId, tags);
            songServiceClient.createSongMetadata(metadata);
            resourceProcessedEventPublisher.publishResourceProcessed(resourceId);
            log.info("Successfully processed resource: resourceId={}", resourceId);
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("Resource not found in resource-service: resourceId={}", resourceId);
        } catch (HttpClientErrorException.Conflict e) {
            log.info("Song metadata already exists for resourceId={}, skipping", resourceId);
        } catch (Mp3ProcessingException e) {
            log.error("Metadata extraction failed for resourceId={}: {}", resourceId, e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error processing resourceId={}", resourceId, e);
        }
    }
}
