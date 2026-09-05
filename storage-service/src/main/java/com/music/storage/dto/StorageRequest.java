package com.music.storage.dto;

import com.music.storage.domain.StorageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record StorageRequest(
        @NotNull(message = "must not be null") StorageType storageType,
        @NotBlank(message = "must not be blank") String bucket,
        @NotBlank(message = "must not be blank") String path) {
}
