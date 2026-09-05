package com.music.resource.client;

import com.music.resource.domain.StorageType;
import com.music.resource.dto.StorageResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Wraps calls to storage-service in a circuit breaker. On failure/open-circuit it falls back to
 * the last successfully retrieved {@link StorageResponse} per {@link StorageType} (near-static
 * data), or a configured default stub if storage-service has never answered successfully.
 */
@Component
public class StorageServiceClient {

    private static final Logger log = LoggerFactory.getLogger(StorageServiceClient.class);

    private final RestClient restClient;
    private final Map<StorageType, StorageResponse> lastKnownGood = new ConcurrentHashMap<>();
    private final Map<StorageType, StorageResponse> defaultStubs;

    public StorageServiceClient(
            RestClient storageServiceRestClient,
            @Value("${storage.stub.staging.bucket:mp3-staging}") String stubStagingBucket,
            @Value("${storage.stub.staging.path:/staging}") String stubStagingPath,
            @Value("${storage.stub.permanent.bucket:mp3-permanent}") String stubPermanentBucket,
            @Value("${storage.stub.permanent.path:/permanent}") String stubPermanentPath) {
        this.restClient = storageServiceRestClient;
        this.defaultStubs = Map.of(
                StorageType.STAGING, new StorageResponse(null, StorageType.STAGING, stubStagingBucket, stubStagingPath),
                StorageType.PERMANENT,
                        new StorageResponse(null, StorageType.PERMANENT, stubPermanentBucket, stubPermanentPath));
    }

    @CircuitBreaker(name = "storageService", fallbackMethod = "getStorageFallback")
    public StorageResponse getStorage(StorageType type) {
        List<StorageResponse> storages = restClient
                .get()
                .uri(uriBuilder -> uriBuilder.path("/storages").queryParam("type", type).build())
                .retrieve()
                .body(new ParameterizedTypeReference<List<StorageResponse>>() {});
        if (storages == null || storages.isEmpty()) {
            throw new IllegalStateException("No storage configured for type " + type);
        }
        StorageResponse storage = storages.get(0);
        lastKnownGood.put(type, storage);
        return storage;
    }

    private StorageResponse getStorageFallback(StorageType type, Throwable throwable) {
        log.warn(
                "storage-service unavailable, falling back to stub storage details for type={}: {}",
                type,
                throwable.getMessage());
        return lastKnownGood.getOrDefault(type, defaultStubs.get(type));
    }
}
