package com.music.song.service.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.music.song.exception.BadRequestException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class SongIdParserTest {

    @Test
    void parse_validCsv() {
        Assertions.assertThat(SongIdParser.parseIdsCsv("1,2")).containsExactly(1L, 2L);
    }

    @Test
    void parse_rejectsInvalidValues() {
        assertThatThrownBy(() -> SongIdParser.parseIdsCsv("1,0")).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> SongIdParser.parseIdsCsv("abc")).isInstanceOf(BadRequestException.class);
    }
}
