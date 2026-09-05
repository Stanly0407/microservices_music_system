package com.music.resource.config;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;


@Component
public class KeycloakClientCredentialsTokenSupplier {

    private final RestClient tokenRestClient;
    private final String tokenUri;
    private final String clientId;
    private final String clientSecret;

    private volatile CachedToken cachedToken;

    public KeycloakClientCredentialsTokenSupplier(
            @Value("${keycloak.token-uri}") String tokenUri,
            @Value("${storage-service.client-id}") String clientId,
            @Value("${storage-service.client-secret}") String clientSecret) {
        this.tokenRestClient = RestClient.create();
        this.tokenUri = tokenUri;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public synchronized String getAccessToken() {
        CachedToken token = cachedToken;
        if (token != null && token.isStillValid()) {
            return token.accessToken();
        }
        token = fetchToken();
        cachedToken = token;
        return token.accessToken();
    }

    private CachedToken fetchToken() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);

        TokenResponse response = tokenRestClient
                .post()
                .uri(tokenUri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(TokenResponse.class);
        if (response == null) {
            throw new IllegalStateException("Keycloak returned an empty token response");
        }
        // Refresh a bit early so a request never runs with a token that expires mid-flight.
        Instant expiresAt = Instant.now().plusSeconds(Math.max(response.expiresIn() - 10, 0));
        return new CachedToken(response.accessToken(), expiresAt);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TokenResponse(@JsonProperty("access_token") String accessToken, @JsonProperty("expires_in") long expiresIn) {}

    private record CachedToken(String accessToken, Instant expiresAt) {
        boolean isStillValid() {
            return Instant.now().isBefore(expiresAt);
        }
    }
}
