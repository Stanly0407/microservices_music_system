package com.music.processor.dto;

public record SongMetadataPayload(
        Long resourceId,
        String name,
        String artist,
        String album,
        String duration,
        String year
) {
}
