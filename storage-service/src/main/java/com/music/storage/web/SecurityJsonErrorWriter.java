package com.music.storage.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.music.storage.dto.error.ApiErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

final class SecurityJsonErrorWriter {

    private SecurityJsonErrorWriter() {}

    static void write(HttpServletResponse response, ObjectMapper objectMapper, HttpStatus status, String message)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter()
                .write(objectMapper.writeValueAsString(new ApiErrorResponse(message, String.valueOf(status.value()))));
    }
}
