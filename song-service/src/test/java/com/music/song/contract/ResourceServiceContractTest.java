package com.music.song.contract;

import static org.assertj.core.api.Assertions.assertThat;

import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import com.music.song.client.ResourceServiceClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.web.client.RestClient;

/**
 * Consumer-driven contract test: song-service (consumer) describes what it expects from
 * resource-service's GET /resources/{id} endpoint, the one call ResourceServiceClient makes.
 * Running this test generates a pact file under build/pacts describing the expectation;
 * resource-service replays that same file against its real controller to verify it still holds.
 */
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "resource-service", pactVersion = PactSpecVersion.V3)
class ResourceServiceContractTest {

    @Pact(consumer = "song-service", provider = "resource-service")
    RequestResponsePact resourceExists(PactDslWithProvider builder) {
        return builder
                .given("a resource with id 42 exists")
                .uponReceiving("a request to check whether resource 42 exists")
                .path("/resources/42")
                .method("GET")
                .willRespondWith()
                .status(200)
                .toPact();
    }

    @Pact(consumer = "song-service", provider = "resource-service")
    RequestResponsePact resourceMissing(PactDslWithProvider builder) {
        return builder
                .given("a resource with id 999 does not exist")
                .uponReceiving("a request to check whether resource 999 exists")
                .path("/resources/999")
                .method("GET")
                .willRespondWith()
                .status(404)
                .body(new PactDslJsonBody()
                        .stringType("errorMessage")
                        .stringType("errorCode"))
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "resourceExists")
    void whenResourceExists_clientReturnsTrue(au.com.dius.pact.consumer.MockServer mockServer) {
        ResourceServiceClient client = clientFor(mockServer);

        assertThat(client.resourceExists(42)).isTrue();
    }

    @Test
    @PactTestFor(pactMethod = "resourceMissing")
    void whenResourceMissing_clientReturnsFalse(au.com.dius.pact.consumer.MockServer mockServer) {
        ResourceServiceClient client = clientFor(mockServer);

        assertThat(client.resourceExists(999)).isFalse();
    }

    private ResourceServiceClient clientFor(au.com.dius.pact.consumer.MockServer mockServer) {
        RestClient restClient = RestClient.builder().baseUrl(mockServer.getUrl()).build();
        return new ResourceServiceClient(restClient);
    }
}
