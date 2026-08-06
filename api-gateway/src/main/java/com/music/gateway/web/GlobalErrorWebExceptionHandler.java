package com.music.gateway.web;

import java.net.SocketException;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.music.gateway.dto.error.ApiErrorResponse;

import reactor.core.publisher.Mono;

/**
 * <p>{@code @Order(HIGHEST_PRECEDENCE)} is required: without it this bean sorts after (and never
 * runs before) the framework's own unordered {@code WebFluxResponseStatusExceptionHandler}, which
 * silently resolves {@link ResponseStatusException}s (for example, an unmatched route) with an
 * empty-body response before this handler ever sees them.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalErrorWebExceptionHandler implements ErrorWebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalErrorWebExceptionHandler.class);

    private final ObjectMapper objectMapper;

    public GlobalErrorWebExceptionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();
        if (response.isCommitted()) {
            return Mono.error(ex);
        }

        HttpStatus status = resolveStatus(ex);
        if (status.is5xxServerError()) {
            log.error("Unhandled error routing request {}", exchange.getRequest().getPath(), ex);
        }

        ApiErrorResponse body = new ApiErrorResponse(resolveMessage(status, ex), String.valueOf(status.value()));

        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        DataBuffer buffer = response.bufferFactory().wrap(toJsonBytes(body));
        return response.writeWith(Mono.just(buffer));
    }

    private HttpStatus resolveStatus(Throwable ex) {
        if (ex instanceof ResponseStatusException rse) {
            HttpStatus resolved = HttpStatus.resolve(rse.getStatusCode().value());
            return resolved != null ? resolved : HttpStatus.INTERNAL_SERVER_ERROR;
        }
        if (isConnectivityFailure(ex)) {
            return HttpStatus.SERVICE_UNAVAILABLE;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private boolean isConnectivityFailure(Throwable ex) {
        // Covers ConnectException (nothing listening) and NoRouteToHostException (container/
        // network gone) alike — both are SocketException, but neither is a subtype of the other.
        for (Throwable current = ex; current != null; current = current.getCause()) {
            if (current instanceof SocketException) {
                return true;
            }
        }
        return false;
    }

    private String resolveMessage(HttpStatus status, Throwable ex) {
        return switch (status) {
            case NOT_FOUND -> "The requested resource was not found";
            case SERVICE_UNAVAILABLE -> "The requested service is currently unavailable. Please try again later";
            default -> status.is5xxServerError()
                    ? "An error occurred on the server"
                    : ex.getMessage() != null ? ex.getMessage() : status.getReasonPhrase();
        };
    }

    private byte[] toJsonBytes(ApiErrorResponse body) {
        try {
            return objectMapper.writeValueAsBytes(body);
        } catch (JsonProcessingException e) {
            return "{\"errorMessage\":\"An error occurred on the server\",\"errorCode\":\"500\"}"
                    .getBytes(StandardCharsets.UTF_8);
        }
    }
}
