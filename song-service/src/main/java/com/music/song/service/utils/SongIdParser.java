package com.music.song.service.utils;

import java.util.ArrayList;
import java.util.List;

import com.music.song.exception.BadRequestException;

public final class SongIdParser {

    public static final String VALID_ID_REG_EXP = "[1-9]\\d*";
    private static final int MAX_CSV_LENGTH = 200;

    private SongIdParser() {
    }

    public static long parseStringId(String rawId) {
        if (rawId == null || !rawId.matches(VALID_ID_REG_EXP)) {
            throw new BadRequestException(
                    "Invalid value '" + rawId + "' for ID. Must be a positive integer");
        }
        try {
            return Long.parseLong(rawId);
        } catch (NumberFormatException ex) {
            throw new BadRequestException(
                    "Invalid value '" + rawId + "' for ID. Must be a positive integer");
        }
    }

    public static List<Long> parseIdsCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            throw new BadRequestException("CSV string format is invalid");
        }
        if (csv.length() > MAX_CSV_LENGTH) {
            throw new BadRequestException(
                    "CSV string is too long: received " + csv.length() + " characters, maximum allowed is " + MAX_CSV_LENGTH);
        }
        String[] parts = csv.split(",");
        List<Long> ids = new ArrayList<>();
        for (String part : parts) {
            ids.add(parsePart(part.trim()));
        }
        return ids;
    }

    private static long parsePart(String value) {
        if (!value.matches(VALID_ID_REG_EXP)) {
            throw new BadRequestException(
                    "Invalid ID format: '" + value + "'. Only positive integers are allowed");
        }
        return Long.parseLong(value);
    }
}
