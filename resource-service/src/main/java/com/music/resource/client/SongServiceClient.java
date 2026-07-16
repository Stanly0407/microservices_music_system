package com.music.resource.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class SongServiceClient {

    private final RestClient restClient;

    public SongServiceClient(RestClient songServiceRestClient) {
        this.restClient = songServiceRestClient;
    }

    public void deleteSongMetadata(long resourceId) {
        restClient
                .delete()
                .uri(uriBuilder -> uriBuilder.path("/songs").queryParam("id", resourceId).build())
                .retrieve()
                .toBodilessEntity();
    }
}
