package com.music.song.service;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.music.song.client.ResourceServiceClient;
import com.music.song.domain.SongMetadata;
import com.music.song.dto.DeletedRecordIdsResponse;
import com.music.song.dto.RecordIdResponse;
import com.music.song.dto.SongMetadataRequest;
import com.music.song.dto.SongMetadataResponse;
import com.music.song.exception.ConflictException;
import com.music.song.exception.NotFoundException;
import com.music.song.repository.SongMetadataRepository;
import com.music.song.service.utils.SongIdParser;

@Service
public class SongMetadataService {

    private static final Logger log = LoggerFactory.getLogger(SongMetadataService.class);

    private final SongMetadataRepository repository;

    public SongMetadataService(
            SongMetadataRepository repository, ResourceServiceClient resourceServiceClient) {
        this.repository = repository;
    }

    @Transactional
    public RecordIdResponse create(SongMetadataRequest request) {
        if (repository.existsById(request.id())) {
            log.warn("Song metadata already exists for id={}, rejecting", request.id());
            throw new ConflictException("Metadata for resource ID=" + request.id() + " already exists");
        }
        SongMetadata saved = repository.save(new SongMetadata(
                request.id(),
                request.name(),
                request.artist(),
                request.album(),
                request.duration(),
                request.year()));
        log.info("Song metadata saved: id={}, name={}, artist={}", saved.getId(), saved.getName(), saved.getArtist());
        return new RecordIdResponse(saved.getId());
    }

    @Transactional(readOnly = true)
    public SongMetadataResponse getById(String rawId) {
        long id = SongIdParser.parseStringId(rawId);
        return repository
                .findById(id)
                .map(SongMetadataResponse::from)
                .orElseThrow(() -> {
                    log.warn("Song metadata not found: id={}", id);
                    return new NotFoundException("Song metadata for ID=" + id + " not found");
                });
    }

    @Transactional
    public DeletedRecordIdsResponse deleteByIds(String idCsv) {
        List<Long> ids = SongIdParser.parseIdsCsv(idCsv);
        List<Long> deletedIds = new ArrayList<>();
        for (Long id : ids) {
            if (repository.existsById(id)) {
                repository.deleteById(id);
                deletedIds.add(id);
            }
        }
        log.info("Deleted song metadata: ids={}", deletedIds);
        return new DeletedRecordIdsResponse(deletedIds);
    }
}
