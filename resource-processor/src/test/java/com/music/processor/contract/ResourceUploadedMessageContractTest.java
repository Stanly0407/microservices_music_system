package com.music.processor.contract;

import static org.assertj.core.api.Assertions.assertThat;

import au.com.dius.pact.consumer.MessagePactBuilder;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.consumer.junit5.ProviderType;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.messaging.Message;
import au.com.dius.pact.core.model.messaging.MessagePact;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.music.processor.dto.ResourceUploadedEvent;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Consumer-driven contract test for the asynchronous side of the system: resource-processor
 * (consumer) describes the shape of the RabbitMQ message it expects resource-service to publish
 * after an upload. Running this test writes a message pact under build/pacts; resource-service
 * replays it against the actual event it constructs to confirm the two DTOs (each service keeps
 * its own copy) haven't drifted apart.
 */
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "resource-service", providerType = ProviderType.ASYNCH, pactVersion = PactSpecVersion.V3)
class ResourceUploadedMessageContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @au.com.dius.pact.core.model.annotations.Pact(consumer = "resource-processor")
    MessagePact resourceUploadedEvent(MessagePactBuilder builder) {
        PactDslJsonBody body = new PactDslJsonBody().numberType("resourceId", 42L);

        return builder.given("a resource with id 42 was uploaded")
                .expectsToReceive("a resource uploaded event")
                .withContent(body)
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "resourceUploadedEvent")
    void listenerCanDeserializeTheEvent(List<Message> messages) throws Exception {
        ResourceUploadedEvent event =
                objectMapper.readValue(messages.get(0).contentsAsBytes(), ResourceUploadedEvent.class);

        assertThat(event.resourceId()).isEqualTo(42L);
    }
}
