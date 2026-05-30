package com.music.resource.service;

import com.music.resource.exception.BadRequestException;
import java.util.ArrayList;
import java.util.List;

public final class DeleteIdsParser {

    private static final int MAX_CSV_LENGTH = 200;

    private DeleteIdsParser() {
    }

    public static List<Long> parse(String csv) {
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
            if (!trimmed.matches("[1-9]\\d*")) {
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
