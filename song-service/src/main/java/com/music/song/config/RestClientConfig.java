package com.music.song.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    @LoadBalanced
    RestClient.Builder restClientBuilder(ObjectProvider<RestClientCustomizer> customizers) {
        RestClient.Builder builder = RestClient.builder();
        customizers.orderedStream().forEach(customizer -> customizer.customize(builder));
        return builder;
    }

    @Bean
    RestClient resourceServiceRestClient(@LoadBalanced RestClient.Builder builder) {
        return builder.baseUrl("http://resource-service").build();
    }
}