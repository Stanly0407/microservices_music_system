package com.music.processor.client;

import com.music.processor.dto.SongMetadataPayload;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

@Component
public class SongServiceClient {

    private final RestClient restClient;

    public SongServiceClient(@Qualifier("songServiceRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Retryable(
            retryFor = {HttpServerErrorException.class, ResourceAccessException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2))
    public void createSongMetadata(SongMetadataPayload payload) {
        restClient
                .post()
                .uri("/songs")
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .toBodilessEntity();
    }
}
