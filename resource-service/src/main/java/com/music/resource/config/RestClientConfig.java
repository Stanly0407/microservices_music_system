package com.music.resource.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    @LoadBalanced
    RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    RestClient songServiceRestClient(@LoadBalanced RestClient.Builder builder) {
        return builder.baseUrl("http://song-service").build();
    }

    @Bean
    RestClient storageServiceRestClient(@LoadBalanced RestClient.Builder builder) {
        return builder.baseUrl("http://storage-service").build();
    }
}