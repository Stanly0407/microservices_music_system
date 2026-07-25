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
| rabbitmq | 5672 / 15672 | Message broker (AMQP / management UI) |
| localstack | 4566 | S3 emulator for MP3 binary storage |

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

## Run Locally (IDE + Docker infrastructure)

Start infrastructure (non-Java services that can't run without Docker):
```bash
docker compose up -d resource-db song-db localstack rabbitmq
```

Then run all Spring Boot services in IDE or terminal. Start Eureka first — other services register with it on startup:
```bash
./gradlew :eureka-server:bootRun
./gradlew :api-gateway:bootRun
./gradlew :resource-service:bootRun
./gradlew :resource-processor:bootRun
./gradlew :song-service:bootRun
```

Send requests through the gateway (`localhost:8080`) — it resolves service instances via Eureka.

---

## Check Service Status

| What | URL |
|---|---|
| All registered services | http://localhost:8761 |
| RabbitMQ queues and exchanges | http://localhost:15672 (guest / guest) |
| LocalStack S3 health | http://localhost:4566/_localstack/health |
| Docker container statuses | `docker compose ps` |
| Service logs | `docker compose logs -f resource-service` |

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

### Everything at once

```bash
# unit + integration + component + contract for every module (excludes e2e)
./gradlew build

# a single test class
./gradlew :resource-service:test --tests "com.music.resource.service.ResourceServiceTest"
```

Results:
- Console summary after the run
- Per-class XML reports: `<module>/build/test-results/test/`
- HTML report: `<module>/build/reports/tests/test/index.html` (e2e: `.../reports/tests/e2eTest/index.html`)


## Troubleshooting

- **Service can't connect on startup** — wait ~20 s for dependencies (DB, RabbitMQ, Eureka) to become healthy, then `docker compose restart <service>`
- **Port in use** — `netstat -ano | findstr :8081` (Windows) or `lsof -i :8081` (Mac/Linux)
- **S3 bucket missing** — resource-service creates it automatically on startup via the SDK; no AWS CLI needed
- **DB schema mismatch after S3 migration** — run `docker compose down -v && docker compose up -d --build` to reinitialise with the current schema
