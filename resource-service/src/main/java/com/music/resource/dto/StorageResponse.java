package com.music.resource.dto;

import com.music.resource.domain.StorageType;

public record StorageResponse(Long id, StorageType storageType, String bucket, String path) {
}
