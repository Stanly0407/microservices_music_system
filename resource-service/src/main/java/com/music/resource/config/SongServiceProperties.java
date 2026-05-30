package com.music.resource.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "song-service")
public record SongServiceProperties(String baseUrl) {
}
