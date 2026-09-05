package com.music.gateway.web;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Mono;


@Component
public class RestServerAccessDeniedHandler implements ServerAccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public RestServerAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, AccessDeniedException denied) {
        return SecurityJsonErrorWriter.write(
                exchange, objectMapper, HttpStatus.FORBIDDEN, "You do not have permission to perform this action");
    }
}
