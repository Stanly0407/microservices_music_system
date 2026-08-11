package com.music.storage.service.utils;

import com.music.storage.exception.BadRequestException;
import java.util.ArrayList;
import java.util.List;

public final class RecordIdsParser {

    public static final String VALID_ID_REG_EXP = "[1-9]\\d*";
    private static final int MAX_CSV_LENGTH = 200;

    private RecordIdsParser() {
    }

    private static BadRequestException invalidCsvIdException(String rawId) {
        String value = rawId == null ? "null" : rawId;
        return new BadRequestException("Invalid ID format: '" + value + "'. Only positive integers are allowed");
    }

    public static List<Long> parseIdsCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            throw new BadRequestException("CSV string format is invalid");
        }
        int actualLength = csv.length();
        if (actualLength > MAX_CSV_LENGTH) {
            throw new BadRequestException(
                    "CSV string is too long: received " + actualLength + " characters, maximum allowed is 200");
        }
        String[] parts = csv.split(",");
        List<Long> ids = new ArrayList<>();
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.matches(VALID_ID_REG_EXP)) {
                throw invalidCsvIdException(trimmed);
            }
            try {
                ids.add(Long.parseLong(trimmed));
            } catch (NumberFormatException ex) {
                throw invalidCsvIdException(trimmed);
            }
        }
        return ids;
    }
}
