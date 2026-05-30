package com.music.song.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "resource-service")
public record ResourceServiceProperties(String baseUrl) {
}
