package com.music.processor.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class Mp3MetadataExtractorTest {

    @Test
    void formatSecondsToMmSs_usesLeadingZeros() {
        assertThat(Mp3MetadataExtractor.formatSecondsToMmSs(3.2653103)).isEqualTo("00:03");
        assertThat(Mp3MetadataExtractor.formatSecondsToMmSs(132.4)).isEqualTo("02:12");
        assertThat(Mp3MetadataExtractor.formatSecondsToMmSs(179.0)).isEqualTo("02:59");
    }
}
