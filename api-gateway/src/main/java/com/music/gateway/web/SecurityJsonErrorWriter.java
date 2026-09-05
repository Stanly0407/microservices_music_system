package com.music.gateway.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.music.gateway.dto.error.ApiErrorResponse;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

final class SecurityJsonErrorWriter {

    private SecurityJsonErrorWriter() {}

    static Mono<Void> write(ServerWebExchange exchange, ObjectMapper objectMapper, HttpStatus status, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        ApiErrorResponse body = new ApiErrorResponse(message, String.valueOf(status.value()));
        DataBuffer buffer = response.bufferFactory().wrap(toJsonBytes(objectMapper, body));
        return response.writeWith(Mono.just(buffer));
    }

    private static byte[] toJsonBytes(ObjectMapper objectMapper, ApiErrorResponse body) {
        try {
            return objectMapper.writeValueAsBytes(body);
        } catch (Exception e) {
            return "{\"errorMessage\":\"An error occurred on the server\",\"errorCode\":\"500\"}"
                    .getBytes(StandardCharsets.UTF_8);
        }
    }
}
