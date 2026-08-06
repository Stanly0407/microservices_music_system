package com.music.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.music.e2e.GatewayClient.Response;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.io.IOException;
import java.time.Duration;

public class ApiGatewayRoutingSteps {

    private final GatewayClient gateway = new GatewayClient();
    private final EurekaClient eureka = new EurekaClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private Response lastResponse;

    @When("I request {string} through the gateway")
    public void iRequestThroughTheGateway(String path) {
        lastResponse = gateway.get(path);
    }

    @Then("the gateway responds with status {int}")
    public void theGatewayRespondsWithStatus(int expectedStatus) {
        assertThat(lastResponse.status()).isEqualTo(expectedStatus);
    }

    @And("the response body has errorMessage {string}")
    public void theResponseBodyHasErrorMessage(String expectedMessage) throws IOException {
        assertThat(fieldOf("errorMessage")).isEqualTo(expectedMessage);
    }

    @And("the response body has errorCode {string}")
    public void theResponseBodyHasErrorCode(String expectedCode) throws IOException {
        assertThat(fieldOf("errorCode")).isEqualTo(expectedCode);
    }

    private String fieldOf(String field) throws IOException {
        JsonNode json = objectMapper.readTree(lastResponse.body());
        return json.get(field).asText();
    }

    @Then("eventually {string} has {int} instances registered as UP in Eureka")
    public void eventuallyHasInstancesRegisteredAsUpInEureka(String appName, int expectedCount) {
        await().atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofSeconds(2))
                .untilAsserted(() -> assertThat(eureka.countUpInstances(appName)).isEqualTo(expectedCount));
    }
}
