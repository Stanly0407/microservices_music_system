package com.music.storage.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "storages")
public class Storage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "storage_type", nullable = false, length = 20)
    private StorageType storageType;

    @Column(nullable = false, length = 255)
    private String bucket;

    @Column(nullable = false, length = 255)
    private String path;

    protected Storage() {
    }

    public Storage(StorageType storageType, String bucket, String path) {
        this.storageType = storageType;
        this.bucket = bucket;
        this.path = path;
    }

    public Long getId() {
        return id;
    }

    public StorageType getStorageType() {
        return storageType;
    }

    public String getBucket() {
        return bucket;
    }

    public String getPath() {
        return path;
    }
}
