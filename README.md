# Music Microservices

Spring Boot 3.4 / Java 21 / PostgreSQL 17 / RabbitMQ / LocalStack S3 / Docker.

## Services

| Service | Port | Role |
|---|---|---|
| api-gateway | 8080 | Single entry point — routes all client requests |
| resource-service | 8081 | Accepts MP3 uploads, stores binary in S3, publishes upload event to RabbitMQ |
| resource-processor | 8083 | Listens for upload events, extracts MP3 metadata via Tika, saves to song-service |
| song-service | 8082 | CRUD for song metadata (2 replicas) |
| eureka-server | 8761 | Service registry |
| keycloak | 8090 | OAuth2/OIDC authorization server — issues and validates JWTs for the Storage API |
| storage-ui | 4200 | Angular UI for the Storage API (login via Keycloak, view/add/delete storages) |
| rabbitmq | 5672 / 15672 | Message broker (AMQP / management UI) |
| localstack | 4566 | S3 emulator for MP3 binary storage |
| elasticsearch | 9200 | Log storage (indexed by service, queried from Grafana) |
| logstash | 5000 | Receives JSON logs (TCP) from every service and writes them to Elasticsearch |
| prometheus | 9090 | Scrapes `/actuator/prometheus` from every service (via Eureka service discovery) |
| grafana | 3000 | Dashboards for metrics (Prometheus) and logs (Elasticsearch) — `admin` / `admin` |

---

## Run in Docker

```bash
docker compose up -d --build
```

---


## Check Service Status

| What | URL |
|---|---|
| All registered services | http://localhost:8761 |
| Storage UI (Angular) | http://localhost:4200 — sign in via Keycloak with `admin-user` / `admin-pass` (role `ADMIN`) or `regular-user` / `user-pass` (role `USER`), see [keycloak/import/realm-export.json](keycloak/import/realm-export.json) |
| Keycloak Admin Console | http://localhost:8090 (admin / admin) — manage the `music-system` realm, users, roles, clients |
| RabbitMQ queues and exchanges | http://localhost:15672 (guest / guest) |
| LocalStack S3 health | http://localhost:4566/_localstack/health |
| eureka-server health | http://localhost:8761/actuator/health |
| Docker container statuses (includes health) | `docker compose ps` |
| Service logs | `docker compose logs -f resource-service` |

---

## Observability: Logging, Monitoring, Tracing

Every service logs structured JSON (via `logstash-logback-encoder`) both to the console and over
TCP to Logstash, which indexes it into Elasticsearch as `app-logs-<service>-<date>`. Every service also
exposes `/actuator/prometheus`, scraped by Prometheus through Eureka service discovery. Grafana is
the single UI for both: a Prometheus data source for metrics dashboards, and an Elasticsearch data
source for searching/correlating logs.

| What | URL |
|---|---|
| Grafana (dashboards + log search) | http://localhost:3000 (admin / admin) |
| Prometheus targets | http://localhost:9090/targets |
| Elasticsearch | http://localhost:9200 |

Pre-provisioned Grafana dashboards (folder "Music Microservices"):
- **JVM Metrics** — heap memory, GC pause time, live threads, CPU usage, per service.
- **API Gateway Performance** — request rate, p95 latency, error rate and status-code breakdown per route.

### Tracing

Every incoming request gets a trace ID (Micrometer Tracing / Brave), automatically propagated through downstream `RestClient` calls and RabbitMQ messages, and injected into every log line via MDC (`traceId`, `spanId`). Uploading a file returns the trace ID in the `X-Trace-Id` response header — use it as a free-text search in Grafana's Elasticsearch Explore view (against the
`app-logs-*` index, field `traceId`) to see that request's full path across api-gateway, resource-service, RabbitMQ, resource-processor and song-service in one query.

---

## Security (OAuth2 / JWT)

The Storage API is protected with OAuth2/JWT, backed by a self-hosted Keycloak (realm
`music-system`, auto-imported from [keycloak/import/realm-export.json](keycloak/import/realm-export.json)).
Two realm roles exist:

| Role | Access |
|---|---|
| `ADMIN` | `GET`, `POST`, `DELETE` `/storages` |
| `USER` | `GET` `/storages` only. `POST`/`DELETE` `/storages` - return `403` |

---

## API Endpoints

All endpoints are reachable through the gateway (`localhost:8080`).

### Resource Service

| Method | Path | Description | Calls other service |
|---|---|---|---|
| POST | /resources | Upload MP3 binary (`Content-Type: audio/mpeg`) → `{"id": 1}` | Publishes event to RabbitMQ → resource-processor picks it up asynchronously |
| GET | /resources/{id} | Download MP3 binary (`audio/mpeg`) | — |
| DELETE | /resources?id=1,2,3 | Delete MP3s → `{"ids": [1,2,3]}` | Calls song-service `DELETE /songs` to remove matching metadata |

### Song Service

| Method | Path | Description | Calls other service |
|---|---|---|---|
| POST | /songs | Save song metadata `{id, name, artist, album, duration, year}` → `{"id": 1}` | — |
| GET | /songs/{id} | Get song metadata | — |
| DELETE | /songs?id=1,2,3 | Delete song metadata records → `{"ids": [1,2,3]}` | — |

### Resource Processor

No REST endpoints. Runs as a background listener on `resource.uploaded.queue`.

On each upload event: calls `GET /resources/{id}` (resource-service) → extracts ID3 tags via Apache Tika → calls `POST /songs` (song-service).

### Storage Service

Protected with OAuth2/JWT — see [Security](#security-oauth2--jwt) above. `GET` requires role `ADMIN` or `USER`; `POST`/`DELETE` require `ADMIN` (`403` otherwise).

| Method | Path | Description | Calls other service |
|---|---|---|---|
| GET | /storages | List storages, optional `?type=STAGING\|PERMANENT` filter → `[{"id":1,"storageType":"STAGING","bucket":"...","path":"..."}]` | — |
| POST | /storages | Register a storage location `{storageType, bucket, path}` → `{"id": 1}` | — |
| DELETE | /storages?id=1,2,3 | Delete storage records → `{"ids": [1,2,3]}` | — |

resource-service calls this API internally (`GET /storages?type=...`) via an OAuth2 client-credentials service account to resolve where to put each MP3 (STAGING on upload, PERMANENT once processed) — see [Upload Flow](#upload-flow).

---

## Upload Flow

```
POST /resources (binary MP3)
  → resource-service stores file in S3
  → saves record to resource-db
  → publishes ResourceUploadedEvent to RabbitMQ
  → returns {"id": 1} immediately

  [async] resource-processor receives event
    → fetches binary from resource-service
    → extracts metadata with Tika
    → posts metadata to song-service
```

```

## Running Tests

See [TESTING_STRATEGY.md](TESTING_STRATEGY.md) for the rationale behind each layer. Run from the
repo root (`./gradlew` in git bash, `.\gradlew.bat` in PowerShell).

| Layer | Command | Notes |
|---|---|---|
| Unit | `./gradlew :resource-service:test --tests "com.music.resource.service.*" --tests "com.music.resource.web.*"`<br>`./gradlew :resource-processor:test --tests "com.music.processor.service.*"`<br>`./gradlew :song-service:test --tests "com.music.song.service.utils.*"` | No Spring context |
| Integration | `./gradlew :song-service:test --tests "com.music.song.repository.*"` | `SongMetadataRepositoryIT` against a real Postgres (Zonky embedded-postgres) |
| Component | `./gradlew :song-service:test --tests "com.music.song.component.RunCucumberTest"` | Full Spring context; Cucumber scenarios in [song_metadata.feature](song-service/src/test/resources/features/song_metadata.feature) |
| Contract | `./gradlew :song-service:test --tests "com.music.song.contract.*"`<br>`./gradlew :resource-processor:test --tests "com.music.processor.contract.*"`<br>`./gradlew :resource-service:test --tests "com.music.resource.contract.*"` | Pact JVM. The `resource-service` run also re-runs the full `song-service`/`resource-processor` suites first — provider verification pulls their pacts via a Gradle `dependsOn`, not affected by `--tests` |
| E2E | `./gradlew :e2e-tests:e2eTest` | Needs the full stack up first (`docker compose up -d --build`, wait ~20-30s for Eureka registration). Against a local gateway: add `-De2e.gatewayUrl=http://localhost:8080 -De2e.eurekaUrl=http://localhost:8761` (skip — needs 2 `song-service` replicas) |

**Everything at once** (unit + integration + component + contract, excludes e2e): `./gradlew build`

Reports: console summary, per-class XML in `<module>/build/test-results/test/`, HTML in
`<module>/build/reports/tests/test/index.html` (e2e: `.../reports/tests/e2eTest/index.html`).

