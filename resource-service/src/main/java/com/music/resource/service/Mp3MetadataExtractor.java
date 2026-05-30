package com.music.resource.service;

import com.music.resource.dto.SongMetadataPayload;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.mp3.Mp3Parser;
import org.apache.tika.sax.BodyContentHandler;
import com.music.resource.exception.BadRequestException;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

@Component
public class Mp3MetadataExtractor {

    private static final String DURATION_KEY = "xmpDM:duration";

    private final Mp3Parser mp3Parser = new Mp3Parser();

    public Map<String, String> extractTags(byte[] mp3Data) {
        Metadata metadata = new Metadata();
        try (InputStream inputStream = new ByteArrayInputStream(mp3Data)) {
            mp3Parser.parse(inputStream, new BodyContentHandler(), metadata, new ParseContext());
        } catch (IOException | SAXException | TikaException ex) {
            throw new BadRequestException("The request body is invalid MP3");
        }
        return toTagMap(metadata);
    }

    public SongMetadataPayload toSongMetadata(long resourceId, Map<String, String> tags) {
        String name = firstNonBlank(tags, "title", "dc:title", "name");
        String artist = firstNonBlank(tags, "xmpDM:artist", "artist", "Author", "xmpDM:composer");
        String album = firstNonBlank(tags, "album", "xmpDM:album");
        String duration = tags.get(DURATION_KEY);
        String year = extractYear(tags);

        if (name == null || artist == null || album == null || duration == null || year == null) {
            throw new BadRequestException("MP3 file does not contain required metadata tags");
        }
        return new SongMetadataPayload(resourceId, name, artist, album, duration, year);
    }

    private Map<String, String> toTagMap(Metadata metadata) {
        Map<String, String> tags = new LinkedHashMap<>();
        for (String name : metadata.names()) {
            String value = metadata.get(name);
            if (value != null && !value.isBlank()) {
                tags.put(name, value);
            }
        }
        formatDuration(tags);
        return tags;
    }

    private void formatDuration(Map<String, String> tags) {
        String rawDuration = tags.get(DURATION_KEY);
        if (rawDuration == null || rawDuration.isBlank()) {
            return;
        }
        try {
            double seconds = Double.parseDouble(rawDuration.trim());
            tags.put(DURATION_KEY, formatSecondsToMmSs(seconds));
        } catch (NumberFormatException ignored) {
            // Keep original value when parsing fails.
        }
    }

    static String formatSecondsToMmSs(double seconds) {
        long totalSeconds = Math.round(seconds);
        long minutes = totalSeconds / 60;
        long secs = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, secs);
    }

    private String firstNonBlank(Map<String, String> tags, String... keys) {
        for (String key : keys) {
            String value = tags.get(key);
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private String extractYear(Map<String, String> tags) {
        String year = firstNonBlank(tags, "year", "xmpDM:releaseYear");
        if (year != null && year.matches("^(19\\d{2}|20\\d{2})$")) {
            return year;
        }
        String date = firstNonBlank(tags, "date", "xmpDM:releaseDate", "creation date");
        if (date != null) {
            String digits = date.replaceAll("\\D", "");
            if (digits.length() >= 4) {
                String candidate = digits.substring(0, 4);
                if (candidate.matches("^(19\\d{2}|20\\d{2})$")) {
                    return candidate;
                }
            }
        }
        return null;
    }
}
