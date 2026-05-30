package com.music.song.repository;

import com.music.song.domain.SongMetadata;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SongMetadataRepository extends JpaRepository<SongMetadata, Long> {
}
