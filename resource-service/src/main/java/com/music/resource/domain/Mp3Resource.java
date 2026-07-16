package com.music.resource.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

    @Column(nullable = false)
    private String storageKey;

    protected Mp3Resource() {
    }

    public Mp3Resource(String storageKey) {
        this.storageKey = storageKey;
    }

    public Long getId() {
        return id;
    }

    public String getStorageKey() {
        return storageKey;
    }
}
