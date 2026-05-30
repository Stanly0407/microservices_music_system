package com.music.resource.dto;

public record SongMetadataPayload(
        Long id, String name, String artist, String album, String duration, String year) {
}
