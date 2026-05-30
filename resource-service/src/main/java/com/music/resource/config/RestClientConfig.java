package com.music.resource.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    RestClient songServiceRestClient(SongServiceProperties properties) {
        return RestClient.builder().baseUrl(properties.baseUrl()).build();
    }
}
