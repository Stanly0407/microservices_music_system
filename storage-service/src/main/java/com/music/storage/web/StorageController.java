package com.music.storage.web;

import com.music.storage.domain.StorageType;
import com.music.storage.dto.DeletedStorageIdsResponse;
import com.music.storage.dto.StorageIdResponse;
import com.music.storage.dto.StorageRequest;
import com.music.storage.dto.StorageResponse;
import com.music.storage.service.StorageService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/storages")
public class StorageController {

    private final StorageService storageService;

    public StorageController(StorageService storageService) {
        this.storageService = storageService;
    }

    @PostMapping
    public ResponseEntity<StorageIdResponse> create(@Valid @RequestBody StorageRequest request) {
        return ResponseEntity.ok(storageService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<StorageResponse>> getAll(
            @RequestParam(name = "type", required = false) StorageType type) {
        return ResponseEntity.ok(storageService.getAll(type));
    }

    @DeleteMapping
    public ResponseEntity<DeletedStorageIdsResponse> delete(@RequestParam("id") String idCsv) {
        return ResponseEntity.ok(storageService.deleteByIds(idCsv));
    }
}
