package com.music.processor.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

@Component
public class ResourceServiceClient {

    private final RestClient restClient;

    public ResourceServiceClient(@Qualifier("resourceServiceRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Retryable(
            retryFor = {HttpServerErrorException.class, ResourceAccessException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2))
    public byte[] getResourceData(long resourceId) {
        return restClient
                .get()
                .uri("/resources/{id}", resourceId)
                .accept(MediaType.valueOf("audio/mpeg"))
                .retrieve()
                .body(byte[].class);
    }
}
