package com.music.storage.service;

import com.music.storage.domain.Storage;
import com.music.storage.domain.StorageType;
import com.music.storage.dto.DeletedStorageIdsResponse;
import com.music.storage.dto.StorageIdResponse;
import com.music.storage.dto.StorageRequest;
import com.music.storage.dto.StorageResponse;
import com.music.storage.repository.StorageRepository;
import com.music.storage.service.utils.RecordIdsParser;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StorageService {

    private final StorageRepository repository;

    public StorageService(StorageRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public StorageIdResponse create(StorageRequest request) {
        Storage saved = repository.save(new Storage(request.storageType(), request.bucket(), request.path()));
        return new StorageIdResponse(saved.getId());
    }

    @Transactional(readOnly = true)
    public List<StorageResponse> getAll(StorageType storageType) {
        List<Storage> storages =
                storageType != null ? repository.findByStorageType(storageType) : repository.findAll();
        return storages.stream().map(StorageResponse::from).toList();
    }

    @Transactional
    public DeletedStorageIdsResponse deleteByIds(String idCsv) {
        List<Long> ids = RecordIdsParser.parseIdsCsv(idCsv);
        List<Long> deletedIds = new ArrayList<>();
        for (Long id : ids) {
            if (repository.existsById(id)) {
                repository.deleteById(id);
                deletedIds.add(id);
            }
        }
        return new DeletedStorageIdsResponse(deletedIds);
    }
}
