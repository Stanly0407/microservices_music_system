package com.music.resource.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.music.resource.exception.BadRequestException;
import org.junit.jupiter.api.Test;

class DeleteIdsParserTest {

    @Test
    void parse_validCsv() {
        assertThat(DeleteIdsParser.parse("1,2,3")).containsExactly(1L, 2L, 3L);
        assertThat(DeleteIdsParser.parse(" 10 , 20 ")).containsExactly(10L, 20L);
    }

    @Test
    void parse_rejectsInvalidValues() {
        assertThatThrownBy(() -> DeleteIdsParser.parse("1,a")).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> DeleteIdsParser.parse("0,1")).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> DeleteIdsParser.parse("1.5")).isInstanceOf(BadRequestException.class);
    }

//    @Test
//    void parse_rejectsTooLongCsv() {
//        String csv = "1,".repeat(100);
//        assertThatThrownBy(() -> DeleteIdsParser.parse(csv)).isInstanceOf(BadRequestException.class);
//    }
}
