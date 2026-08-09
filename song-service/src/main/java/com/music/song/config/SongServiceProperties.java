package com.music.song.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "song.service")
public class SongServiceProperties {

    private String testConfigMessage;

    public String getTestConfigMessage() {
        return testConfigMessage;
    }

    public void setTestConfigMessage(String testConfigMessage) {
        this.testConfigMessage = testConfigMessage;
    }
}
