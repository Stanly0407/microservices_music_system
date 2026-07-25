package com.music.resource.service.utils;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class Mp3UtilTest {

    @Test
    void looksLikeMp3_id3Header_returnsTrue() {
        assertThat(Mp3Util.looksLikeMp3(new byte[] {'I', 'D', '3', 0x03, 0x00})).isTrue();
    }

    @Test
    void looksLikeMp3_id3HeaderOnly_ignoresRestOfBytes() {
        assertThat(Mp3Util.looksLikeMp3(new byte[] {'I', 'D', '3'})).isTrue();
    }

    @Test
    void looksLikeMp3_mpegFrameSync_returnsTrue() {
        assertThat(Mp3Util.looksLikeMp3(new byte[] {(byte) 0xFF, (byte) 0xFB, 0x00})).isTrue();
    }

    @Test
    void looksLikeMp3_mpegFrameSyncMinimalLength_returnsTrue() {
        assertThat(Mp3Util.looksLikeMp3(new byte[] {(byte) 0xFF, (byte) 0xE0})).isTrue();
    }

    @Test
    void looksLikeMp3_emptyData_returnsFalse() {
        assertThat(Mp3Util.looksLikeMp3(new byte[0])).isFalse();
    }

    @Test
    void looksLikeMp3_singleByte_returnsFalse() {
        assertThat(Mp3Util.looksLikeMp3(new byte[] {(byte) 0xFF})).isFalse();
    }

    @Test
    void looksLikeMp3_randomBytesMatchingNeitherPattern_returnsFalse() {
        assertThat(Mp3Util.looksLikeMp3(new byte[] {0x00, 0x01, 0x02})).isFalse();
    }

    @Test
    void looksLikeMp3_firstByteNotSyncOrId3_returnsFalse() {
        assertThat(Mp3Util.looksLikeMp3(new byte[] {(byte) 0xAB, (byte) 0xFB, 0x00})).isFalse();
    }
}
