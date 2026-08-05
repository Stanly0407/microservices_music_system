package com.music.resource.contract;

import static org.mockito.Mockito.when;

import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import au.com.dius.pact.provider.spring.junit5.MockMvcTestTarget;
import au.com.dius.pact.provider.spring.junit5.PactVerificationSpringProvider;
import com.music.resource.dto.ResourceDataResponse;
import com.music.resource.exception.NotFoundException;
import com.music.resource.service.ResourceService;
import com.music.resource.web.ResourceController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Provider-side verification: replays the pact recorded by song-service (see
 * song-service's ResourceServiceContractTest) against the real {@link ResourceController}
 * to confirm resource-service still honours what the consumer expects. 
 * The pact file itself is not committed: the {@code copyConsumerPacts}
 * Gradle task (see build.gradle) pulls the freshest copy out of song-service's build output
 * before this test runs, so verification always checks what the consumer currently expects.
 */
@Provider("resource-service")
@PactFolder("build/pacts-from-consumers/song-service")
@WebMvcTest(ResourceController.class)
class ResourceServiceProviderContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ResourceService resourceService;

    @BeforeEach
    void setUp(PactVerificationContext context) {
        context.setTarget(new MockMvcTestTarget(mockMvc));
    }

    @State("a resource with id 42 exists")
    void resourceExists() {
        when(resourceService.getResourceData("42")).thenReturn(new ResourceDataResponse(new byte[] {1, 2, 3}));
    }

    @State("a resource with id 999 does not exist")
    void resourceMissing() {
        when(resourceService.getResourceData("999"))
                .thenThrow(new NotFoundException("Resource with ID=999 not found"));
    }

    @TestTemplate
    @ExtendWith(PactVerificationSpringProvider.class)
    void pactVerificationTestTemplate(PactVerificationContext context) {
        context.verifyInteraction();
    }
}
