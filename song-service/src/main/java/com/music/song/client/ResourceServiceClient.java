package com.music.song.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class ResourceServiceClient {

    private final RestClient restClient;

    public ResourceServiceClient(RestClient resourceServiceRestClient) {
        this.restClient = resourceServiceRestClient;
    }

    public boolean resourceExists(long resourceId) {
        try {
            restClient
                    .get()
                    .uri("/resources/{id}", resourceId)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                return false;
            }
            throw ex;
        }
    }
}
