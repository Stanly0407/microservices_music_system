package com.music.storage.dto;

import com.music.storage.domain.Storage;
import com.music.storage.domain.StorageType;

public record StorageResponse(Long id, StorageType storageType, String bucket, String path) {

    public static StorageResponse from(Storage storage) {
        return new StorageResponse(storage.getId(), storage.getStorageType(), storage.getBucket(), storage.getPath());
    }
}
