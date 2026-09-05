package com.music.song.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.music.song.config.SongServiceProperties;

// Exposes a Config Server-sourced value so /actuator/refresh's effect is observable without a restart.
@RestController
public class ConfigInfoController {

    private final SongServiceProperties songServiceProperties;

    public ConfigInfoController(SongServiceProperties songServiceProperties) {
        this.songServiceProperties = songServiceProperties;
    }

    @GetMapping("/config-info")
    public SongServiceProperties configInfo() {
        return songServiceProperties;
    }
}
