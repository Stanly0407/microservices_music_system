package com.music.e2e;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;


public class EurekaClient {

    private static final String BASE_URL = System.getProperty("e2e.eurekaUrl", "http://localhost:8761");

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public int countUpInstances(String appName) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(BASE_URL + "/eureka/apps/" + appName))
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return 0;
            }
            return countUp(objectMapper.readTree(response.body()).path("application").path("instance"));
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException(
                    "Could not reach eureka-server at " + BASE_URL
                            + " — is the stack up? Run `docker compose up -d --build` first.",
                    e);
        }
    }

    private int countUp(JsonNode instances) {
        if (instances.isArray()) {
            int count = 0;
            for (JsonNode instance : instances) {
                if (isUp(instance)) {
                    count++;
                }
            }
            return count;
        }
        if (instances.isObject() && isUp(instances)) {
            return 1;
        }
        return 0;
    }

    private boolean isUp(JsonNode instance) {
        return "UP".equals(instance.path("status").asText());
    }
}
