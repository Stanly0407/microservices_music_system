package com.music.song.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record SongMetadataRequest(
        @NotNull(message = Msg.ID_REQUIRED) @Positive(message = Msg.ID) Long id,
        @NotNull(message = Msg.NAME_REQUIRED) @Size(min = 1, max = 100, message = Msg.NAME) String name,
        @NotNull(message = Msg.ARTIST_REQUIRED) @Size(min = 1, max = 100, message = Msg.ARTIST) String artist,
        @NotNull(message = Msg.ALBUM_REQUIRED) @Size(min = 1, max = 100, message = Msg.ALBUM) String album,
        @NotNull(message = Msg.DURATION_REQUIRED) @Pattern(regexp = "^\\d{2}:[0-5]\\d$", message = Msg.DURATION) String duration,
        @NotNull(message = Msg.YEAR_REQUIRED) @Pattern(regexp = "^(19\\d{2}|20\\d{2})$", message = Msg.YEAR) String year
) {}
