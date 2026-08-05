package com.music.song.component.steps;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.music.song.repository.SongMetadataRepository;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

public class SongMetadataSteps {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private SongMetadataRepository repository;

    @Autowired
    private ObjectMapper objectMapper;

    private ResponseEntity<String> lastResponse;

    @Before
    public void resetDatabase() {
        repository.deleteAll();
        lastResponse = null;
    }

    @Given("the song catalog is empty")
    public void theSongCatalogIsEmpty() {
        assertThat(repository.count()).isZero();
    }

    @Given("song metadata already exists for resource {long}")
    public void songMetadataAlreadyExistsForResource(long id) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("id", String.valueOf(id));
        body.put("name", "Existing " + id);
        body.put("artist", "Artist " + id);
        body.put("album", "Album " + id);
        body.put("duration", "03:00");
        body.put("year", "2000");

        ResponseEntity<String> response = post("/songs", body);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @When("I create song metadata for resource {long} with:")
    public void iCreateSongMetadataForResourceWith(long id, DataTable table) {
        Map<String, String> body = new LinkedHashMap<>(table.asMap(String.class, String.class));
        body.put("id", String.valueOf(id));
        lastResponse = post("/songs", body);
    }

    @When("I look up song metadata for resource {long}")
    public void iLookUpSongMetadataForResourceLong(long id) {
        lastResponse = restTemplate.getForEntity("/songs/{id}", String.class, id);
    }

    @When("I look up song metadata for resource {string}")
    public void iLookUpSongMetadataForResourceString(String id) {
        lastResponse = restTemplate.getForEntity("/songs/{id}", String.class, id);
    }

    @When("I delete song metadata for resources {string}")
    public void iDeleteSongMetadataForResources(String idsCsv) {
        lastResponse = restTemplate.exchange(
                "/songs?id={idsCsv}", HttpMethod.DELETE, HttpEntity.EMPTY, String.class, idsCsv);
    }

    @Then("the response status is {int}")
    public void theResponseStatusIs(int expectedStatus) {
        assertThat(lastResponse.getStatusCode().value()).isEqualTo(expectedStatus);
    }

    @And("the response contains the created record id {long}")
    public void theResponseContainsTheCreatedRecordId(long id) throws Exception {
        JsonNode json = objectMapper.readTree(lastResponse.getBody());
        assertThat(json.get("id").asLong()).isEqualTo(id);
    }

    @And("song metadata for resource {long} can be retrieved with:")
    public void songMetadataForResourceCanBeRetrievedWith(long id, DataTable table) throws Exception {
        ResponseEntity<String> response = restTemplate.getForEntity("/songs/{id}", String.class, id);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        JsonNode json = objectMapper.readTree(response.getBody());
        Map<String, String> expected = table.asMap(String.class, String.class);
        expected.forEach((field, value) -> assertThat(json.get(field).asText()).isEqualTo(value));
    }

    @And("the validation error mentions field {string}")
    public void theValidationErrorMentionsField(String field) throws Exception {
        JsonNode json = objectMapper.readTree(lastResponse.getBody());
        assertThat(json.get("details").has(field)).isTrue();
    }

    @And("the response lists deleted ids {string}")
    public void theResponseListsDeletedIds(String idsCsv) throws Exception {
        JsonNode json = objectMapper.readTree(lastResponse.getBody());
        List<Long> expected = Arrays.stream(idsCsv.split(","))
                .map(String::trim)
                .map(Long::parseLong)
                .toList();
        List<Long> actual = new ArrayList<>();
        json.get("ids").forEach(node -> actual.add(node.asLong()));
        assertThat(actual).containsExactlyElementsOf(expected);
    }

    @And("song metadata for resource {long} no longer exists")
    public void songMetadataForResourceNoLongerExists(long id) {
        assertThat(repository.existsById(id)).isFalse();
    }

    private ResponseEntity<String> post(String uri, Map<String, String> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.postForEntity(uri, new HttpEntity<>(body, headers), String.class);
    }
}
