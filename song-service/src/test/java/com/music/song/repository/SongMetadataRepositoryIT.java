package com.music.song.repository;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.music.song.domain.SongMetadata;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;


@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SongMetadataRepositoryIT {

    private static final EmbeddedPostgres POSTGRES = startPostgres();

    @Autowired
    private SongMetadataRepository repository;

    private static EmbeddedPostgres startPostgres() {
        try {
            EmbeddedPostgres postgres = EmbeddedPostgres.start();
            try (Connection connection = postgres.getPostgresDatabase().getConnection();
                    Statement statement = connection.createStatement()) {
                statement.execute(Files.readString(Path.of("../init-scripts/song-db/init.sql")));
            }
            return postgres;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to initialise embedded Postgres schema", e);
        }
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:postgresql://localhost:" + POSTGRES.getPort() + "/postgres");
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "postgres");
    }

    @AfterAll
    static void stopPostgres() throws IOException {
        POSTGRES.close();
    }

    @Test
    void save_persistsAllFields_andFindByIdReturnsThem() {
        repository.save(new SongMetadata(1L, "Song", "Artist", "Album", "03:30", "2020"));

        Optional<SongMetadata> found = repository.findById(1L);

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Song");
        assertThat(found.get().getArtist()).isEqualTo("Artist");
        assertThat(found.get().getAlbum()).isEqualTo("Album");
        assertThat(found.get().getDuration()).isEqualTo("03:30");
        assertThat(found.get().getYear()).isEqualTo("2020");
    }

    @Test
    void findById_unknownId_returnsEmpty() {
        assertThat(repository.findById(999L)).isEmpty();
    }

    @Test
    void existsById_reflectsPersistedState() {
        assertThat(repository.existsById(2L)).isFalse();

        repository.save(new SongMetadata(2L, "Song", "Artist", "Album", "03:30", "2020"));

        assertThat(repository.existsById(2L)).isTrue();
    }

    @Test
    void deleteById_removesRecord() {
        repository.save(new SongMetadata(3L, "Song", "Artist", "Album", "03:30", "2020"));

        repository.deleteById(3L);

        assertThat(repository.existsById(3L)).isFalse();
    }

    @Test
    void save_withAlreadyAssignedId_updatesExistingRowInsteadOfInsertingDuplicate() {
        repository.saveAndFlush(new SongMetadata(4L, "Original", "Artist", "Album", "03:30", "2020"));

        repository.saveAndFlush(new SongMetadata(4L, "Updated", "Artist", "Album", "03:30", "2020"));

        assertThat(repository.count()).isEqualTo(1);
        assertThat(repository.findById(4L)).get().extracting(SongMetadata::getName).isEqualTo("Updated");
    }

    @Test
    void save_missingRequiredColumn_throwsDataIntegrityViolationExceptionOnFlush() {
        SongMetadata missingName = new SongMetadata(5L, null, "Artist", "Album", "03:30", "2020");

        assertThatThrownBy(() -> repository.saveAndFlush(missingName))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
