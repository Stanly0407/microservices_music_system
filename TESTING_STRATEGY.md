# Testing Strategy

## Approach: layered, not uniform

The strategy will combine five layers testing pyramid, each catching a different class of bug. This system will not be tested with one dominant technique (e.g. "mostly unit tests" or "mostly end-to-end"). Each services has different risk areas (database/S3/broker integration, cross-service HTTP calls, an async messaging handoff) and no single test type catches failures in all of them. 

Moving up the testing pyramid, tests get slower, more expensive to maintain, and cover more of the real system at once. For example, a bug in `Mp3Util.looksLikeMp3` should fail a unit test in milliseconds - it shouldn't require the full  stack to surface.

## What each layer is responsible for

| Layer | Answers the question | Tooling | Scope in this system |
|---|---|---|---|
| **Unit** | Does this class do the right thing in isolation? | JUnit 5 | Business/utility logic with no Spring context (`ResourceService` validation branches, `RecordIdsParser`, `Mp3Util`, `SongIdParser`, exception-mapping in the `GlobalExceptionHandler`) |
| **Integration** | Does this service talk correctly to *its own* infrastructure? | JUnit 5 + a real, disposable instance of the dependency (for example, embedded Postgres for JPA repositories) | **Implemented:** `SongMetadataRepositoryIT` (song-service) against a real Postgres engine via Zonky embedded-postgres, schema loaded from `init-scripts/song-db/init.sql`. |
| **Component** | Does this service behave correctly as a whole, from a business perspective? | Cucumber + Spring Boot Test | One service's full context up, driven through its real API/queue entry point, calls to *other* services replaced with stubs so the scenario is deterministic |
| **Contract** | Will this service and its collaborator still work together if either changes independently? | Pact JVM (consumer-driven contracts) | Cross-service interaction, both communication styles used in this system. **Implemented:** (1) synchronous HTTP — `song-service` (consumer) vs `resource-service` (provider) for `GET /resources/{id}`, the call `ResourceServiceClient.resourceExists` makes; (2) messaging — `resource-processor` (consumer) vs `resource-service` (producer/provider) for the `ResourceUploadedEvent` RabbitMQ message, using Pact's message-pact support instead of a real broker |
| **E2E** | Does the real, fully wired system deliver the user-facing outcome? | Cucumber | **Implemented:** `e2e-tests` module - upload a song end to end (gateway → resource-service → RabbitMQ → resource-processor → song-service) and delete-cascades-to-song-metadata |

## Why this combination, not just unit + integration

**Unit** and **integration tests** alone would not be enougth to confirm the correct operation of each service, because the real cause of failures in this system may be in individual components. For example, a REST client sending a field the receiving service doesn't expect, a message schema in a queue being rejected because it doesn't match, or an asynchronous chain of loading, processing and metadata creation silently losing messages. Detecting these issues requires component, contract, and end-to-end tests, and they can't be replaced by additional unit tests.

**Component tests** validate business scenarios: uploading a valid MP3 stores it and publishes an event, deleting a resource also removes its song metadata, etc.

**Contract tests** are what let `resource-service`, `resource-processor`, and `song-service` be deployed independently without a full regression pass across all three every time. The consumer's test records real expectations as a pact file (JSON); the provider replays that same file — against its actual controller for HTTP (service layer mocked per state), or against the actual event DTO serialized the same way the real publisher does for messaging — to verify it still holds, no need to run both services together.

**Stub/pact propagation:** rather than a Pact Broker (another container this system would depend on — the same Docker-on-Windows reliability concern noted above for Testcontainers), propagation is wired directly into Gradle: each provider module has a `copyConsumerPacts` task that `dependsOn` its consumers' `test` tasks and copies their freshly generated `build/pacts/*.json` into the provider's own `build/pacts-from-consumers/<consumer>/`; the provider's `test` task depends on that copy task. So `./gradlew :resource-service:test` always re-runs `song-service` and `resource-processor`'s consumer tests first, regenerates their pacts, and verifies against that current copy. The trade-off: this only works inside the monorepo (a provider can't be verified against a consumer it can't build).

**E2E tests** are the only layer that proves the deployed, wired-together system works, including things no lower layer can see: Eureka registration, gateway routing. Kept deliberately small because this layer is slow and the failures it catches should be rare if the layers below are doing their job.
