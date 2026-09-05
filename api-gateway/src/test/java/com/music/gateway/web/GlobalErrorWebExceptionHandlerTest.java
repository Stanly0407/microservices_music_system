package com.music.gateway.web;

import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;

import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.test.StepVerifier;

class GlobalErrorWebExceptionHandlerTest {

    private final GlobalErrorWebExceptionHandler handler = new GlobalErrorWebExceptionHandler(new ObjectMapper());

    @Test
    void undefinedRoute_returnsFriendlyNotFound() {
        ServerWebExchange exchange = exchange("/does-not-exist");

        handle(exchange, new ResponseStatusException(HttpStatus.NOT_FOUND, "No matching handler"));

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(exchange.getResponse().getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(bodyOf(exchange))
                .isEqualTo("{\"errorMessage\":\"The requested resource was not found\",\"errorCode\":\"404\"}");
    }

    @Test
    void noEurekaInstanceAvailable_returnsFriendlyServiceUnavailable() {
        ServerWebExchange exchange = exchange("/songs/1");

        handle(exchange, NotFoundException.create(false, "Unable to find instance for song-service"));

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(bodyOf(exchange)).isEqualTo(
                "{\"errorMessage\":\"The requested service is currently unavailable. Please try again later\","
                        + "\"errorCode\":\"503\"}");
    }

    @Test
    void downstreamConnectionRefused_returnsFriendlyServiceUnavailable() {
        ServerWebExchange exchange = exchange("/songs/1");

        handle(exchange, new RuntimeException("test", new ConnectException("Connection refused")));

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(bodyOf(exchange)).isEqualTo(
                "{\"errorMessage\":\"The requested service is currently unavailable. Please try again later\","
                        + "\"errorCode\":\"503\"}");
    }

    @Test
    void downstreamContainerGone_returnsFriendlyServiceUnavailable() {
        ServerWebExchange exchange = exchange("/songs/1");

        handle(exchange, new RuntimeException("test", new NoRouteToHostException("No route to host")));

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(bodyOf(exchange)).isEqualTo(
                "{\"errorMessage\":\"The requested service is currently unavailable. Please try again later\","
                        + "\"errorCode\":\"503\"}");
    }

    @Test
    void unexpectedException_returnsGenericInternalServerError() {
        ServerWebExchange exchange = exchange("/songs/1");

        handle(exchange, new IllegalStateException("test"));

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(bodyOf(exchange))
                .isEqualTo("{\"errorMessage\":\"An error occurred on the server\",\"errorCode\":\"500\"}");
    }

    private void handle(ServerWebExchange exchange, Throwable error) {
        StepVerifier.create(handler.handle(exchange, error)).verifyComplete();
    }

    private ServerWebExchange exchange(String path) {
        return MockServerWebExchange.from(MockServerHttpRequest.get(URI.create(path).toString()));
    }

    private String bodyOf(ServerWebExchange exchange) {
        return ((MockServerWebExchange) exchange).getResponse().getBodyAsString().block();
    }
}
