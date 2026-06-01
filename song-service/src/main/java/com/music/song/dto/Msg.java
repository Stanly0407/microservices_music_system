package com.music.song.dto;

interface Msg {
    String ID_REQUIRED = "ID is required";
    String ID = "ID must be a positive number";

    String NAME_REQUIRED = "Song name is required";
    String NAME = "Song name must be between 1 and 100 characters";

    String ARTIST_REQUIRED = "Artist name is required";
    String ARTIST = "Artist name must be between 1 and 100 characters";

    String ALBUM_REQUIRED = "Album name is required";
    String ALBUM = "Album name must be between 1 and 100 characters";

    String DURATION_REQUIRED = "Duration is required";
    String DURATION = "Duration must be in mm:ss format with leading zeros";

    String YEAR_REQUIRED = "Year is required";
    String YEAR = "Year must be between 1900 and 2099";
}
