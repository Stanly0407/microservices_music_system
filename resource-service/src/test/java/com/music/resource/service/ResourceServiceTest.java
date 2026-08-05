package com.music.resource.service;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.music.resource.client.SongServiceClient;
import com.music.resource.domain.Mp3Resource;
import com.music.resource.dto.DeletedResourceIdsResponse;
import com.music.resource.dto.ResourceDataResponse;
import com.music.resource.dto.ResourceIdResponse;
import com.music.resource.exception.BadRequestException;
import com.music.resource.exception.NotFoundException;
import com.music.resource.messaging.ResourceEventPublisher;
import com.music.resource.repository.Mp3ResourceRepository;

@ExtendWith(MockitoExtension.class)
class ResourceServiceTest {

    private static final byte[] VALID_MP3 = {'I', 'D', '3', 0, 0, 0};
    private static final byte[] INVALID_DATA = {0x00, 0x01, 0x02};

    @Mock
    private Mp3ResourceRepository repository;

    @Mock
    private SongServiceClient songServiceClient;

    @Mock
    private S3StorageService storageService;

    @Mock
    private ResourceEventPublisher eventPublisher;

    private ResourceService resourceService;

    @BeforeEach
    void setUp() {
        resourceService = new ResourceService(repository, songServiceClient, storageService, eventPublisher);
    }

    @Test
    void upload_validMp3_storesPersistsAndPublishesEvent() {
        when(storageService.store(VALID_MP3)).thenReturn("storage-key");
        when(repository.save(any(Mp3Resource.class))).thenReturn(resourceWithId(42L, "storage-key"));

        ResourceIdResponse response = resourceService.upload(VALID_MP3);

        assertThat(response.id()).isEqualTo(42L);
        verify(storageService).store(VALID_MP3);
        verify(eventPublisher).publishResourceUploaded(42L);
    }

    @Test
    void upload_nullData_throwsBadRequestExceptionAndSkipsCollaborators() {
        assertThatThrownBy(() -> resourceService.upload(null)).isInstanceOf(BadRequestException.class);
        verifyNoInteractions(storageService, repository, eventPublisher);
    }

    @Test
    void upload_emptyData_throwsBadRequestExceptionAndSkipsCollaborators() {
        assertThatThrownBy(() -> resourceService.upload(new byte[0])).isInstanceOf(BadRequestException.class);
        verifyNoInteractions(storageService, repository, eventPublisher);
    }

    @Test
    void upload_dataNotLookingLikeMp3_throwsBadRequestExceptionAndSkipsCollaborators() {
        assertThatThrownBy(() -> resourceService.upload(INVALID_DATA)).isInstanceOf(BadRequestException.class);
        verifyNoInteractions(storageService, repository, eventPublisher);
    }

    @Test
    void getResourceData_existingId_returnsStoredBytes() {
        when(repository.findById(7L)).thenReturn(Optional.of(resourceWithId(7L, "storage-key")));
        when(storageService.retrieve("storage-key")).thenReturn(VALID_MP3);

        ResourceDataResponse response = resourceService.getResourceData("7");

        assertThat(response.data()).isEqualTo(VALID_MP3);
    }

    @Test
    void getResourceData_missingId_throwsNotFoundExceptionAndSkipsStorage() {
        when(repository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resourceService.getResourceData("7")).isInstanceOf(NotFoundException.class);
        verifyNoInteractions(storageService);
    }

    @Test
    void getResourceData_invalidRawId_throwsBadRequestExceptionBeforeTouchingRepository() {
        assertThatThrownBy(() -> resourceService.getResourceData("abc")).isInstanceOf(BadRequestException.class);
        verifyNoInteractions(repository, storageService);
    }

    // ---- deleteByIds delete loop ----

    @Test
    void deleteByIds_existingIds_deletesFromSongServiceStorageAndRepositoryForEachId() {
        when(repository.findById(1L)).thenReturn(Optional.of(resourceWithId(1L, "key-1")));
        when(repository.findById(2L)).thenReturn(Optional.of(resourceWithId(2L, "key-2")));

        DeletedResourceIdsResponse response = resourceService.deleteByIds("1,2");

        assertThat(response.ids()).containsExactly(1L, 2L);
        verify(songServiceClient).deleteSongMetadata(1L);
        verify(songServiceClient).deleteSongMetadata(2L);
        verify(storageService).delete("key-1");
        verify(storageService).delete("key-2");
        verify(repository).deleteById(1L);
        verify(repository).deleteById(2L);
    }

    @Test
    void deleteByIds_missingId_skippedWithoutSideEffects() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        DeletedResourceIdsResponse response = resourceService.deleteByIds("1");

        assertThat(response.ids()).isEmpty();
        verifyNoInteractions(songServiceClient, storageService);
        verify(repository, never()).deleteById(any(Long.class));
    }

    @Test
    void deleteByIds_mixOfExistingAndMissingIds_onlyDeletesExistingOne() {
        when(repository.findById(1L)).thenReturn(Optional.of(resourceWithId(1L, "key-1")));
        when(repository.findById(2L)).thenReturn(Optional.empty());

        DeletedResourceIdsResponse response = resourceService.deleteByIds("1,2");

        assertThat(response.ids()).containsExactly(1L);
        verify(songServiceClient).deleteSongMetadata(1L);
        verify(songServiceClient, never()).deleteSongMetadata(2L);
        verify(repository).deleteById(1L);
        verify(repository, never()).deleteById(2L);
    }

    private static Mp3Resource resourceWithId(long id, String storageKey) {
        Mp3Resource resource = new Mp3Resource(storageKey);
        ReflectionTestUtils.setField(resource, "id", id);
        return resource;
    }
}
