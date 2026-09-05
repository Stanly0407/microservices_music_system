package com.music.resource.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "mp3_resources")
public class Mp3Resource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "storage_type", nullable = false, length = 20)
    private StorageType storageType;

    @Column(nullable = false, length = 255)
    private String bucket;

    @Column(nullable = false, length = 500)
    private String path;

    protected Mp3Resource() {
    }

    public Mp3Resource(StorageType storageType, String bucket, String path) {
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

    public void moveTo(StorageType storageType, String bucket, String path) {
        this.storageType = storageType;
        this.bucket = bucket;
        this.path = path;
    }
}
