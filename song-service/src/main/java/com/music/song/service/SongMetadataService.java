package com.music.song.service;

import java.util.ArrayList;
import java.util.List;

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

    private final SongMetadataRepository repository;

    public SongMetadataService(
            SongMetadataRepository repository, ResourceServiceClient resourceServiceClient) {
        this.repository = repository;
    }

    @Transactional
    public RecordIdResponse create(SongMetadataRequest request) {
        if (repository.existsById(request.id())) {
            throw new ConflictException("Metadata for resource ID=" + request.id() + " already exists");
        }
        SongMetadata saved = repository.save(new SongMetadata(
                request.id(),
                request.name(),
                request.artist(),
                request.album(),
                request.duration(),
                request.year()));
        return new RecordIdResponse(saved.getId());
    }

    @Transactional(readOnly = true)
    public SongMetadataResponse getById(String rawId) {
        long id = SongIdParser.parseStringId(rawId);
        return repository
                .findById(id)
                .map(SongMetadataResponse::from)
                .orElseThrow(() -> new NotFoundException("Song metadata for ID=" + id + " not found"));
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
        return new DeletedRecordIdsResponse(deletedIds);
    }
}
