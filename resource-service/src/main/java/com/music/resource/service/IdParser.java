package com.music.resource.service;

import com.music.resource.exception.BadRequestException;

public final class IdParser {

    private IdParser() {
    }

    public static long parsePositiveId(String rawId) {
        if (rawId == null || !rawId.matches("[1-9]\\d*")) {
            throw new BadRequestException("The provided ID is invalid");
        }
        try {
            return Long.parseLong(rawId);
        } catch (NumberFormatException ex) {
            throw new BadRequestException("The provided ID is invalid");
        }
    }
}
