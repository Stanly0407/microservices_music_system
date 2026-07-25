package com.music.resource.service.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;

import com.music.resource.exception.BadRequestException;

class RecordIdsParserTest {

    @Test
    void parseStringId_validPositiveInteger_returnsParsedLong() {
        assertThat(RecordIdsParser.parseStringId("42")).isEqualTo(42L);
    }

    @Test
    void parseStringId_null_throwsBadRequestException() {
        assertThatThrownBy(() -> RecordIdsParser.parseStringId(null)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void parseStringId_nonNumeric_throwsBadRequestException() {
        assertThatThrownBy(() -> RecordIdsParser.parseStringId("abc")).isInstanceOf(BadRequestException.class);
    }

    @Test
    void parseStringId_zero_throwsBadRequestException() {
        assertThatThrownBy(() -> RecordIdsParser.parseStringId("0")).isInstanceOf(BadRequestException.class);
    }

    @Test
    void parseStringId_negative_throwsBadRequestException() {
        assertThatThrownBy(() -> RecordIdsParser.parseStringId("-5")).isInstanceOf(BadRequestException.class);
    }

    @Test
    void parseStringId_leadingZero_throwsBadRequestException() {
        assertThatThrownBy(() -> RecordIdsParser.parseStringId("01")).isInstanceOf(BadRequestException.class);
    }

    @Test
    void parseStringId_tooLargeForLong_throwsBadRequestException() {
        assertThatThrownBy(() -> RecordIdsParser.parseStringId("99999999999999999999"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void parseIdsCsv_validCsv_returnsParsedList() {
        assertThat(RecordIdsParser.parseIdsCsv("1,2,3")).containsExactly(1L, 2L, 3L);
    }

    @Test
    void parseIdsCsv_trimsWhitespaceAroundValues() {
        assertThat(RecordIdsParser.parseIdsCsv("1, 2 ,3")).containsExactly(1L, 2L, 3L);
    }

    @Test
    void parseIdsCsv_null_throwsBadRequestException() {
        assertThatThrownBy(() -> RecordIdsParser.parseIdsCsv(null)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void parseIdsCsv_blank_throwsBadRequestException() {
        assertThatThrownBy(() -> RecordIdsParser.parseIdsCsv("   ")).isInstanceOf(BadRequestException.class);
    }

    @Test
    void parseIdsCsv_exceedsMaxLength_throwsBadRequestException() {
        String tooLong = "1,".repeat(150);
        assertThatThrownBy(() -> RecordIdsParser.parseIdsCsv(tooLong)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void parseIdsCsv_containsInvalidValue_throwsBadRequestException() {
        assertThatThrownBy(() -> RecordIdsParser.parseIdsCsv("1,abc,3")).isInstanceOf(BadRequestException.class);
    }

    @Test
    void parseIdsCsv_containsZeroOrNegativeValue_throwsBadRequestException() {
        assertThatThrownBy(() -> RecordIdsParser.parseIdsCsv("1,0")).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> RecordIdsParser.parseIdsCsv("1,-2")).isInstanceOf(BadRequestException.class);
    }

    @Test
    void parseIdsCsv_valueTooLargeForLong_throwsBadRequestException() {
        assertThatThrownBy(() -> RecordIdsParser.parseIdsCsv("1,99999999999999999999"))
                .isInstanceOf(BadRequestException.class);
    }
}
