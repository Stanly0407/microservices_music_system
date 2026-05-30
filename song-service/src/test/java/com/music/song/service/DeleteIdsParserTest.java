package com.music.song.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.music.song.exception.BadRequestException;
import org.junit.jupiter.api.Test;

class DeleteIdsParserTest {

    @Test
    void parse_validCsv() {
        assertThat(DeleteIdsParser.parse("1,2")).containsExactly(1L, 2L);
    }

    @Test
    void parse_rejectsInvalidValues() {
        assertThatThrownBy(() -> DeleteIdsParser.parse("1,0")).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> DeleteIdsParser.parse("abc")).isInstanceOf(BadRequestException.class);
    }
}
