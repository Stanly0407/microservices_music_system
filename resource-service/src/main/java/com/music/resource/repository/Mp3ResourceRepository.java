package com.music.resource.repository;

import com.music.resource.domain.Mp3Resource;
import org.springframework.data.jpa.repository.JpaRepository;

public interface Mp3ResourceRepository extends JpaRepository<Mp3Resource, Long> {
}
