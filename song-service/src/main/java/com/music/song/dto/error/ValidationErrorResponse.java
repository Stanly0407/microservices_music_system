package com.music.song.dto.error;

import java.util.Map;

public record ValidationErrorResponse(String errorMessage, Map<String, String> details, String errorCode) {
}
