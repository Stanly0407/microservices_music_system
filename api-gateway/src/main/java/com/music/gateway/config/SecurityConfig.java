package com.music.gateway.config;

import com.music.gateway.web.RestServerAccessDeniedHandler;
import com.music.gateway.web.RestServerAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Only {@code /storages/**} requires a valid JWT here.
 * Role checks for that path (ADMIN/USER) are enforced downstream by storage-service.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http,
            RestServerAuthenticationEntryPoint entryPoint,
            RestServerAccessDeniedHandler deniedHandler) {
        http.csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchange ->
                        exchange.pathMatchers("/storages/**").authenticated().anyExchange().permitAll())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults())
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(deniedHandler));
        return http.build();
    }
}
