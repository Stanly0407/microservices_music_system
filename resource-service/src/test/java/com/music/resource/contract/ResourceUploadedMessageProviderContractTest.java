package com.music.resource.contract;

import au.com.dius.pact.provider.MessageAndMetadata;
import au.com.dius.pact.provider.PactVerifyProvider;
import au.com.dius.pact.provider.junit5.MessageTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.music.resource.dto.ResourceUploadedEvent;
import com.music.resource.messaging.ResourceEventPublisher;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Provider-side verification for the async message contract recorded by resource-processor
 * (see resource-processor's ResourceUploadedMessageContractTest). Serializes the real
 * {@link ResourceUploadedEvent} the same way {@link ResourceEventPublisher} hands it to
 * RabbitTemplate/Jackson2JsonMessageConverter, so a field rename or type change on this side
 * would fail verification instead of silently breaking the listener in production. The pact
 * file is pulled fresh from resource-processor's build output by the copyConsumerPacts Gradle
 * task, same mechanism used for the song-service HTTP contract.
 */
@Provider("resource-service")
@PactFolder("build/pacts-from-consumers/resource-processor")
class ResourceUploadedMessageProviderContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp(PactVerificationContext context) {
        context.setTarget(new MessageTestTarget());
    }

    @State("a resource with id 42 was uploaded")
    void resourceUploaded() {
    }

    @PactVerifyProvider("a resource uploaded event")
    MessageAndMetadata resourceUploadedMessage() throws Exception {
        byte[] body = objectMapper.writeValueAsBytes(new ResourceUploadedEvent(42L));
        return new MessageAndMetadata(body, Map.of("contentType", "application/json"));
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void pactVerificationTestTemplate(PactVerificationContext context) {
        context.verifyInteraction();
    }
}
