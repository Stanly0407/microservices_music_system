package com.music.resource.service;

import com.music.resource.client.SongServiceClient;
import com.music.resource.domain.Mp3Resource;
import com.music.resource.dto.DeletedResourceIdsResponse;
import com.music.resource.dto.ResourceIdResponse;
import com.music.resource.dto.ResourceDataResponse;
import com.music.resource.dto.SongMetadataPayload;
import com.music.resource.exception.BadRequestException;
import com.music.resource.exception.InternalServerErrorException;
import com.music.resource.exception.NotFoundException;
import com.music.resource.repository.Mp3ResourceRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.music.resource.service.utils.Mp3Util;
import com.music.resource.service.utils.RecordIdsParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

@Service
public class ResourceService {

    private static final Logger log = LoggerFactory.getLogger(ResourceService.class);
    private static final int COMPENSATION_DELETE_ATTEMPTS = 3;

    private final Mp3ResourceRepository repository;
    private final Mp3MetadataExtractor metadataExtractor;
    private final SongServiceClient songServiceClient;

    public ResourceService(
            Mp3ResourceRepository repository,
            Mp3MetadataExtractor metadataExtractor,
            SongServiceClient songServiceClient) {
        this.repository = repository;
        this.metadataExtractor = metadataExtractor;
        this.songServiceClient = songServiceClient;
    }
    public ResourceIdResponse upload(byte[] data) {
        validateMp3Payload(data);
        Map<String, String> tags = metadataExtractor.extractTags(data);
        Mp3Resource resource = repository.save(new Mp3Resource(data));
        SongMetadataPayload songPayload = metadataExtractor.toSongMetadata(resource.getId(), tags);
        try {
            songServiceClient.createSongMetadata(songPayload);
        } catch (RestClientException ex) {
            compensateFailedUpload(resource.getId(), ex);
            throw new InternalServerErrorException();
        }
        return new ResourceIdResponse(resource.getId());
    }

    private void compensateFailedUpload(long resourceId, Exception songServiceError) {
        for (int attempt = 1; attempt <= COMPENSATION_DELETE_ATTEMPTS; attempt++) {
            try {
                repository.deleteById(resourceId);
                return;
            } catch (Exception deleteError) {
                log.warn(
                        "Compensation delete attempt {} failed for resource id={}",
                        attempt,
                        resourceId,
                        deleteError);
            }
        }
        log.error(
                "Could not delete resource id={} after song-service failure; manual/script cleanup may be required",
                resourceId,
                songServiceError);
    }

    @Transactional(readOnly = true)
    public ResourceDataResponse getResourceData(String rawId) {
        long id = RecordIdsParser.parseStringId(rawId);
        Mp3Resource resource = repository
                .findById(id)
                .orElseThrow(() -> new NotFoundException("Resource with ID=" + id + " not found"));
        return new ResourceDataResponse(resource.getData());
    }

    @Transactional
    public DeletedResourceIdsResponse deleteByIds(String idCsv) {
        List<Long> ids = RecordIdsParser.parseIdsCsv(idCsv);
        List<Long> deletedIds = new ArrayList<>();
        for (Long id : ids) {
            if (repository.existsById(id)) {
                songServiceClient.deleteSongMetadata(id);
                repository.deleteById(id);
                deletedIds.add(id);
            }
        }
        return new DeletedResourceIdsResponse(deletedIds);
    }

    private void validateMp3Payload(byte[] data) {
        if (data == null || data.length == 0 || !Mp3Util.looksLikeMp3(data)) {
            throw new BadRequestException("The request body is invalid MP3");
        }
    }
}
