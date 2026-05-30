package com.music.song.dto;

import com.music.song.domain.SongMetadata;

public record SongMetadataResponse(
        Long id, String name, String artist, String album, String duration, String year) {

    public static SongMetadataResponse from(SongMetadata entity) {
        return new SongMetadataResponse(
                entity.getId(),
                entity.getName(),
                entity.getArtist(),
                entity.getAlbum(),
                entity.getDuration(),
                entity.getYear());
    }
}
