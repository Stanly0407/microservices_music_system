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

Rebuild after code changes:
```bash
docker compose down && docker compose up -d --build
```

Reset databases (wipe all data):
```bash
docker compose down -v && docker compose up -d --build
```

---


## Check Service Status

| What | URL |
|---|---|
| All registered services | http://localhost:8761 |
| RabbitMQ queues and exchanges | http://localhost:15672 (guest / guest) |
| LocalStack S3 health | http://localhost:4566/_localstack/health |
| api-gateway health | http://localhost:8080/actuator/health |
| eureka-server health | http://localhost:8761/actuator/health |
| resource-service / song-service / resource-processor health | not published to host — use `docker compose exec <service> wget -qO- http://localhost:<port>/actuator/health`, or hit `http://localhost:<port>/actuator/health` directly when running that service locally via `bootRun` |
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
- **API Gateway Performance** — request rate, p95 latency, error rate and status-code breakdown per
  route.

### Tracing

Every incoming request gets a trace ID (Micrometer Tracing / Brave), automatically propagated
through downstream `RestClient` calls and RabbitMQ messages, and injected into every log line via
MDC (`traceId`, `spanId`). Uploading a file returns the trace ID in the `X-Trace-Id` response
header — use it as a free-text search in Grafana's Elasticsearch Explore view (against the
`app-logs-*` index, field `traceId`) to see that request's full path across api-gateway,
resource-service, RabbitMQ, resource-processor and song-service in one query.

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

All endpoints are reachable through the gateway (`localhost:8080`) or directly on the service port.

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

---

## Common Commands

```bash
# Build all modules
./gradlew build

# View logs for a specific service
docker compose logs -f resource-processor

# Stop all containers (keeps volumes)
docker compose down

# Restart a single service after a crash
docker compose restart resource-service
```

## Running Tests

See [TESTING_STRATEGY.md](TESTING_STRATEGY.md) for the rationale behind each layer. All commands
below run from the repo root (`./gradlew` in git bash, `.\gradlew.bat` in PowerShell).

### 1. Unit Tests

```bash
./gradlew :resource-service:test --tests "com.music.resource.service.*" --tests "com.music.resource.web.*"
./gradlew :resource-processor:test --tests "com.music.processor.service.*"
./gradlew :song-service:test --tests "com.music.song.service.utils.*"
```

### 2. Integration Tests

`SongMetadataRepositoryIT` is covered with a real Postgres via Zonky embedded-postgres.

```bash
./gradlew :song-service:test --tests "com.music.song.repository.*"
```

### 3. Component Tests

Brings up the whole Spring context + embedded Postgres and runs the Cucumber scenarios in
[song_metadata.feature](song-service/src/test/resources/features/song_metadata.feature).

```bash
./gradlew :song-service:test --tests "com.music.song.component.RunCucumberTest"
```

### 4. Contract Tests

Consumer tests generate a pact file and can run standalone, without any other module:

```bash
./gradlew :song-service:test --tests "com.music.song.contract.*"
./gradlew :resource-processor:test --tests "com.music.processor.contract.*"
```

Note: Provider verification in **resource-service** works differently: the `copyConsumerPacts` task
is wired into `test` via `dependsOn` (see `resource-service/build.gradle`), and that dependency is
not affected by `--tests` filtering. So *any* test run in resource-service first runs the full
`song-service:test` (unit+integration+component+contract) and `resource-processor:test`:

```bash
./gradlew :resource-service:test --tests "com.music.resource.contract.*"
```

### 5. E2E (`e2e-tests` module)

Drives uploading a song critical logic through the real stack
(`api-gateway` -> `resource-service` -> RabbitMQ -> `resource-processor` -> `song-service`), no
stubs.

```bash
# 1. Start the full stack
docker compose up -d --build

# 2. Wait ~20-30s for services to register with Eureka (check http://localhost:8761),
#    then run the suite
./gradlew :e2e-tests:e2eTest
```

To point the same suite at a locally-running gateway instead of Docker, override
`e2e.gatewayUrl`/`e2e.eurekaUrl` (the Eureka-replica-count scenario will fail here, since there's
only one `song-service` instance locally — run it only against Docker):

```bash
./gradlew :e2e-tests:e2eTest -De2e.gatewayUrl=http://localhost:8080 -De2e.eurekaUrl=http://localhost:8761
```

### Everything at once

```bash
# unit + integration + component + contract for every module (excludes e2e)
./gradlew build

```

Results:
- Console summary after the run
- Per-class XML reports: `<module>/build/test-results/test/`
- HTML report: `<module>/build/reports/tests/test/index.html` (e2e: `.../reports/tests/e2eTest/index.html`)

