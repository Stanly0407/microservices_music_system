package com.music.e2e;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;


public class GatewayClient {

    private static final String BASE_URL = System.getProperty("e2e.gatewayUrl", "http://localhost:8080");

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public record Response(int status, String body) {
    }

    public Response uploadResource(byte[] mp3Bytes) {
        return send(HttpRequest.newBuilder(URI.create(BASE_URL + "/resources"))
                .header("Content-Type", "audio/mpeg")
                .POST(HttpRequest.BodyPublishers.ofByteArray(mp3Bytes)));
    }

    public Response getSong(long id) {
        return send(HttpRequest.newBuilder(URI.create(BASE_URL + "/songs/" + id)).GET());
    }

    public Response get(String path) {
        return send(HttpRequest.newBuilder(URI.create(BASE_URL + path)).GET());
    }

    public Response deleteResources(String idsCsv) {
        return send(HttpRequest.newBuilder(URI.create(BASE_URL + "/resources?id=" + idsCsv)).DELETE());
    }

    private Response send(HttpRequest.Builder requestBuilder) {
        try {
            HttpResponse<String> response = http.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
            return new Response(response.statusCode(), response.body());
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException(
                    "Could not reach api-gateway at " + BASE_URL
                            + " — is the stack up? Run `docker compose up -d --build` first.",
                    e);
        }
    }
}
