package com.music.song.service;

import com.music.song.client.ResourceServiceClient;
import com.music.song.domain.SongMetadata;
import com.music.song.dto.DeletedRecordIdsResponse;
import com.music.song.dto.RecordIdResponse;
import com.music.song.dto.SongMetadataRequest;
import com.music.song.dto.SongMetadataResponse;
import com.music.song.exception.BadRequestException;
import com.music.song.exception.ConflictException;
import com.music.song.exception.NotFoundException;
import com.music.song.repository.SongMetadataRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SongMetadataService {

    private final SongMetadataRepository repository;
    private final ResourceServiceClient resourceServiceClient;

    public SongMetadataService(
            SongMetadataRepository repository, ResourceServiceClient resourceServiceClient) {
        this.repository = repository;
        this.resourceServiceClient = resourceServiceClient;
    }

    @Transactional
    public RecordIdResponse create(SongMetadataRequest request) {
        if (repository.existsById(request.id())) {
            throw new ConflictException("Metadata for resource id " + request.id() + " already exists");
        }
        if (!resourceServiceClient.resourceExists(request.id())) {
            throw new BadRequestException("Resource with id " + request.id() + " does not exist");
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
        long id = IdParser.parsePositiveId(rawId);
        return repository
                .findById(id)
                .map(SongMetadataResponse::from)
                .orElseThrow(() -> new NotFoundException("Song metadata with ID=" + id + " not found"));
    }

    @Transactional
    public DeletedRecordIdsResponse deleteByIds(String idCsv) {
        List<Long> ids = DeleteIdsParser.parse(idCsv);
        List<Long> deleted = new ArrayList<>();
        for (Long id : ids) {
            if (repository.existsById(id)) {
                repository.deleteById(id);
                deleted.add(id);
            }
        }
        return new DeletedRecordIdsResponse(deleted);
    }
}
