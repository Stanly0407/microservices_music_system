package com.music.resource.service;

import com.music.resource.client.SongServiceClient;
import com.music.resource.domain.Mp3Resource;
import com.music.resource.dto.DeletedResourceIdsResponse;
import com.music.resource.dto.ResourceIdResponse;
import com.music.resource.dto.ResourceDataResponse;
import com.music.resource.exception.BadRequestException;
import com.music.resource.exception.NotFoundException;
import com.music.resource.messaging.ResourceEventPublisher;
import com.music.resource.repository.Mp3ResourceRepository;
import java.util.ArrayList;
import java.util.List;

import com.music.resource.service.utils.Mp3Util;
import com.music.resource.service.utils.RecordIdsParser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResourceService {

    private final Mp3ResourceRepository repository;
    private final SongServiceClient songServiceClient;
    private final S3StorageService storageService;
    private final ResourceEventPublisher eventPublisher;

    public ResourceService(
            Mp3ResourceRepository repository,
            SongServiceClient songServiceClient,
            S3StorageService storageService,
            ResourceEventPublisher eventPublisher) {
        this.repository = repository;
        this.songServiceClient = songServiceClient;
        this.storageService = storageService;
        this.eventPublisher = eventPublisher;
    }

    public ResourceIdResponse upload(byte[] data) {
        if (data == null || data.length == 0 || !Mp3Util.looksLikeMp3(data)) {
            throw new BadRequestException("The request body is invalid MP3");
        }
        String storageKey = storageService.store(data);
        Mp3Resource resource = repository.save(new Mp3Resource(storageKey));
        eventPublisher.publishResourceUploaded(resource.getId());
        return new ResourceIdResponse(resource.getId());
    }

    @Transactional(readOnly = true)
    public ResourceDataResponse getResourceData(String rawId) {
        long id = RecordIdsParser.parseStringId(rawId);
        Mp3Resource resource = repository
                .findById(id)
                .orElseThrow(() -> new NotFoundException("Resource with ID=" + id + " not found"));
        byte[] data = storageService.retrieve(resource.getStorageKey());
        return new ResourceDataResponse(data);
    }

    @Transactional
    public DeletedResourceIdsResponse deleteByIds(String idCsv) {
        List<Long> ids = RecordIdsParser.parseIdsCsv(idCsv);
        List<Long> deletedIds = new ArrayList<>();
        for (Long id : ids) {
            repository.findById(id).ifPresent(resource -> {
                songServiceClient.deleteSongMetadata(id);
                storageService.delete(resource.getStorageKey());
                repository.deleteById(id);
                deletedIds.add(id);
            });
        }
        return new DeletedResourceIdsResponse(deletedIds);
    }
}
