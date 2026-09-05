package com.music.storage.repository;

import com.music.storage.domain.Storage;
import com.music.storage.domain.StorageType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StorageRepository extends JpaRepository<Storage, Long> {

    List<Storage> findByStorageType(StorageType storageType);
}
