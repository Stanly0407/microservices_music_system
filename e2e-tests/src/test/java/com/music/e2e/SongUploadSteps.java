package com.music.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.music.e2e.GatewayClient.Response;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.Map;

public class SongUploadSteps {

    private final GatewayClient gateway = new GatewayClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private Response lastResponse;
    private long resourceId;

    @Given("the api-gateway is reachable")
    public void theApiGatewayIsReachable() {
        Response response = gateway.getSong(0);
        assertThat(response.status()).isBetween(200, 599);
    }

    @When("I upload the fixture MP3 {string} through the gateway")
    public void iUploadTheFixtureMp3ThroughTheGateway(String fixtureName) {
        lastResponse = gateway.uploadResource(readFixture(fixtureName));
    }

    @Then("the upload is accepted with a resource id")
    public void theUploadIsAcceptedWithAResourceId() throws IOException {
        assertThat(lastResponse.status()).isEqualTo(200);
        resourceId = objectMapper.readTree(lastResponse.body()).get("id").asLong();
    }

    @Then("within {int} seconds the song catalog has metadata for that resource with:")
    public void withinSecondsTheSongCatalogHasMetadataForThatResourceWith(int timeoutSeconds, DataTable table) {
        assertThatSongMetadataEventuallyMatches(timeoutSeconds, table.asMap(String.class, String.class));
    }

    @Given("I have uploaded the fixture MP3 {string} and its metadata is in the catalog")
    public void iHaveUploadedTheFixtureMp3AndItsMetadataIsInTheCatalog(String fixtureName) throws IOException {
        Response uploadResponse = gateway.uploadResource(readFixture(fixtureName));
        assertThat(uploadResponse.status()).isEqualTo(200);
        resourceId = objectMapper.readTree(uploadResponse.body()).get("id").asLong();

        await().atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> assertThat(gateway.getSong(resourceId).status()).isEqualTo(200));
    }

    @When("I delete that resource through the gateway")
    public void iDeleteThatResourceThroughTheGateway() {
        lastResponse = gateway.deleteResources(String.valueOf(resourceId));
        assertThat(lastResponse.status()).isEqualTo(200);
    }

    @Then("the song catalog no longer has metadata for that resource")
    public void theSongCatalogNoLongerHasMetadataForThatResource() {
        await().atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> assertThat(gateway.getSong(resourceId).status()).isEqualTo(404));
    }

    private void assertThatSongMetadataEventuallyMatches(int timeoutSeconds, Map<String, String> expected) {
        await().atMost(Duration.ofSeconds(timeoutSeconds))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    Response response = gateway.getSong(resourceId);
                    assertThat(response.status()).isEqualTo(200);
                    JsonNode json = objectMapper.readTree(response.body());
                    expected.forEach((field, value) -> assertThat(json.get(field).asText()).isEqualTo(value));
                });
    }

    private byte[] readFixture(String name) {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("fixtures/" + name)) {
            if (in == null) {
                throw new IllegalArgumentException("Fixture not found on classpath: fixtures/" + name);
            }
            return in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
