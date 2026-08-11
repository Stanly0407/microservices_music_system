package com.music.resource.service;

import com.music.resource.client.SongServiceClient;
import com.music.resource.client.StorageServiceClient;
import com.music.resource.domain.Mp3Resource;
import com.music.resource.domain.StorageType;
import com.music.resource.dto.DeletedResourceIdsResponse;
import com.music.resource.dto.ResourceIdResponse;
import com.music.resource.dto.ResourceDataResponse;
import com.music.resource.dto.StorageResponse;
import com.music.resource.exception.BadRequestException;
import com.music.resource.exception.NotFoundException;
import com.music.resource.messaging.ResourceEventPublisher;
import com.music.resource.repository.Mp3ResourceRepository;
import java.util.ArrayList;
import java.util.List;

import com.music.resource.service.utils.Mp3Util;
import com.music.resource.service.utils.RecordIdsParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResourceService {

    private static final Logger log = LoggerFactory.getLogger(ResourceService.class);

    private final Mp3ResourceRepository repository;
    private final SongServiceClient songServiceClient;
    private final S3StorageService storageService;
    private final StorageServiceClient storageServiceClient;
    private final ResourceEventPublisher eventPublisher;

    public ResourceService(
            Mp3ResourceRepository repository,
            SongServiceClient songServiceClient,
            S3StorageService storageService,
            StorageServiceClient storageServiceClient,
            ResourceEventPublisher eventPublisher) {
        this.repository = repository;
        this.songServiceClient = songServiceClient;
        this.storageService = storageService;
        this.storageServiceClient = storageServiceClient;
        this.eventPublisher = eventPublisher;
    }

    public ResourceIdResponse upload(byte[] data) {
        if (data == null || data.length == 0 || !Mp3Util.looksLikeMp3(data)) {
            throw new BadRequestException("The request body is invalid MP3");
        }
        StorageResponse staging = storageServiceClient.getStorage(StorageType.STAGING);
        String key = storageService.store(staging.bucket(), staging.path(), data);
        Mp3Resource resource = repository.save(new Mp3Resource(StorageType.STAGING, staging.bucket(), key));
        eventPublisher.publishResourceUploaded(resource.getId());
        return new ResourceIdResponse(resource.getId());
    }

    @Transactional(readOnly = true)
    public ResourceDataResponse getResourceData(String rawId) {
        long id = RecordIdsParser.parseStringId(rawId);
        Mp3Resource resource = repository
                .findById(id)
                .orElseThrow(() -> new NotFoundException("Resource with ID=" + id + " not found"));
        byte[] data = storageService.retrieve(resource.getBucket(), resource.getPath());
        return new ResourceDataResponse(data);
    }

    @Transactional
    public DeletedResourceIdsResponse deleteByIds(String idCsv) {
        List<Long> ids = RecordIdsParser.parseIdsCsv(idCsv);
        List<Long> deletedIds = new ArrayList<>();
        for (Long id : ids) {
            repository.findById(id).ifPresent(resource -> {
                songServiceClient.deleteSongMetadata(id);
                storageService.delete(resource.getBucket(), resource.getPath());
                repository.deleteById(id);
                deletedIds.add(id);
            });
        }
        return new DeletedResourceIdsResponse(deletedIds);
    }

    @Transactional
    public void markProcessed(long resourceId) {
        Mp3Resource resource = repository.findById(resourceId).orElse(null);
        if (resource == null) {
            log.warn("Resource not found while marking as processed: resourceId={}", resourceId);
            return;
        }
        if (resource.getStorageType() == StorageType.PERMANENT) {
            log.info("Resource already PERMANENT, skipping: resourceId={}", resourceId);
            return;
        }
        StorageResponse permanent = storageServiceClient.getStorage(StorageType.PERMANENT);
        String newKey =
                storageService.move(resource.getBucket(), resource.getPath(), permanent.bucket(), permanent.path());
        resource.moveTo(StorageType.PERMANENT, permanent.bucket(), newKey);
        repository.save(resource);
        log.info("Resource moved to PERMANENT storage: resourceId={}", resourceId);
    }
}
