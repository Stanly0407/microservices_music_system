package com.music.gateway.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class RequestLoggingGlobalFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingGlobalFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        log.info("Routing request: {} {}", request.getMethod(), request.getURI().getPath());
        return chain.filter(exchange).doOnSuccess(v -> {
            HttpStatusCode status = exchange.getResponse().getStatusCode();
            int statusCode = status != null ? status.value() : 0;
            if (status != null && status.isError()) {
                log.warn(
                        "Request failed: {} {} -> {}",
                        request.getMethod(),
                        request.getURI().getPath(),
                        statusCode);
            } else {
                log.info(
                        "Request completed: {} {} -> {}",
                        request.getMethod(),
                        request.getURI().getPath(),
                        statusCode);
            }
        });
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
