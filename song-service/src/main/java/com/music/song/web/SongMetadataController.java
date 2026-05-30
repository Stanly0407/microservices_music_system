package com.music.song.web;

import com.music.song.dto.DeletedRecordIdsResponse;
import com.music.song.dto.RecordIdResponse;
import com.music.song.dto.SongMetadataRequest;
import com.music.song.dto.SongMetadataResponse;
import com.music.song.service.SongMetadataService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/songs")
public class SongMetadataController {

    private final SongMetadataService songMetadataService;

    public SongMetadataController(SongMetadataService songMetadataService) {
        this.songMetadataService = songMetadataService;
    }

    @PostMapping
    public ResponseEntity<RecordIdResponse> create(@Valid @RequestBody SongMetadataRequest request) {
        return ResponseEntity.ok(songMetadataService.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SongMetadataResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(songMetadataService.getById(id));
    }

    @DeleteMapping
    public ResponseEntity<DeletedRecordIdsResponse> delete(@RequestParam("id") String idCsv) {
        return ResponseEntity.ok(songMetadataService.deleteByIds(idCsv));
    }
}
