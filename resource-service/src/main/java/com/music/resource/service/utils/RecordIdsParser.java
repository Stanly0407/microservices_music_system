package com.music.resource.service.utils;

import com.music.resource.exception.BadRequestException;

import java.util.ArrayList;
import java.util.List;

public final class RecordIdsParser {

    public static final String VALID_ID_REG_EXP = "[1-9]\\d*";
    private static final int MAX_CSV_LENGTH = 200;

    private RecordIdsParser() {
    }

    public static long parseStringId(String rawId) {
        if (rawId == null || !rawId.matches(VALID_ID_REG_EXP)) {
            throw new BadRequestException("The provided ID is invalid");
        }
        try {
            return Long.parseLong(rawId);
        } catch (NumberFormatException ex) {
            throw new BadRequestException("The provided ID is invalid");
        }
    }

    public static List<Long> parseIdsCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            throw new BadRequestException("CSV string format is invalid");
        }
        if (csv.length() > MAX_CSV_LENGTH) {
            throw new BadRequestException("CSV string format is invalid or exceeds length restrictions");
        }
        String[] parts = csv.split(",");
        List<Long> ids = new ArrayList<>();
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.matches(VALID_ID_REG_EXP)) {
                throw new BadRequestException("CSV string format is invalid");
            }
            try {
                ids.add(Long.parseLong(trimmed));
            } catch (NumberFormatException ex) {
                throw new BadRequestException("CSV string format is invalid");
            }
        }
        return ids;
    }
}
