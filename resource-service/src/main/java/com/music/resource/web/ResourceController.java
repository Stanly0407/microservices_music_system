package com.music.resource.web;

import com.music.resource.dto.DeletedRecordIdsResponse;
import com.music.resource.dto.RecordIdResponse;
import com.music.resource.service.ResourceService;
import org.springframework.http.MediaType;
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
@RequestMapping("/resources")
public class ResourceController {

    private final ResourceService resourceService;

    public ResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @PostMapping(consumes = "audio/mpeg")
    public ResponseEntity<RecordIdResponse> upload(@RequestBody byte[] body) {
        return ResponseEntity.ok(resourceService.upload(body));
    }

    @GetMapping(value = "/{id}", produces = "audio/mpeg")
    public ResponseEntity<byte[]> download(@PathVariable String id) {
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("audio/mpeg"))
                .body(resourceService.getResourceData(id).data());
    }

    @DeleteMapping
    public ResponseEntity<DeletedRecordIdsResponse> delete(@RequestParam("id") String idCsv) {
        return ResponseEntity.ok(resourceService.deleteByIds(idCsv));
    }
}
