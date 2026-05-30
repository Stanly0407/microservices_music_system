# Music Microservices

Unified Gradle multi-module project (Spring Boot 3.4 / Java 21 / PostgreSQL).

```
mikroservices-music/
├── gradle/wrapper/
├── resource-service/
├── song-service/
├── gradlew
├── gradlew.bat
├── settings.gradle
├── build.gradle
├── compose.yaml
└── .gitignore
```

| Service | Port | Base path |
|---------|------|-----------|
| Resource Service | 8081 | `/resources` |
| Song Service | 8082 | `/songs` |

## Prerequisites

- Java 21
- Docker (PostgreSQL only; services run locally with Gradle)

## Database

Each microservice uses its own PostgreSQL 16 instance (Alpine), started from the root `compose.yaml`:

| Service | Container | Host port | Database | User |
|---------|-----------|-----------|----------|------|
| Resource Service | `music-postgres-resource` | 5433 | `resource_db` | `resource_user` |
| Song Service | `music-postgres-song` | 5434 | `song_db` | `song_user` |

Schema management:

- **Hibernate** `ddl-auto: update` — tables are created/updated automatically from JPA entities on startup.
- **No** Flyway, Liquibase, `schema.sql`, or `data.sql` (`spring.sql.init.mode: never`).

Start databases:

```bash
docker compose up -d
```

Wait until both containers are healthy, then start the Spring Boot services.

## Run services locally

Terminal 1 — Song Service (start first):

```bash
gradlew :song-service:bootRun
```

Terminal 2 — Resource Service:

```bash
gradlew :resource-service:bootRun
```

## Resource Service API

### Upload resource

`POST /resources`  
Content-Type: `audio/mpeg`  
Body: raw MP3 bytes

Response `200 OK`:

```json
{ "id": 1 }
```

### Get resource

`GET /resources/{id}`  
Returns MP3 bytes (`audio/mpeg`).

### Delete resources

`DELETE /resources?id=1,2`  
CSV of positive integer IDs (max 200 characters). Missing IDs are ignored.

Response `200 OK`:

```json
{ "ids": [1, 2] }
```

Deleting a resource cascades to Song Service metadata for that ID.

## Song Service API

### Create song metadata

`POST /songs`

```json
{
  "id": 1,
  "name": "We are the champions",
  "artist": "Queen",
  "album": "News of the world",
  "duration": "02:59",
  "year": "1977"
}
```

Response `200 OK`:

```json
{ "id": 1 }
```

`id` must match an existing resource. All fields are required and validated.

### Get song metadata

`GET /songs/{id}`

Returns metadata for the resource ID:

```json
{
  "id": 1,
  "name": "We are the champions",
  "artist": "Queen",
  "album": "News of the world",
  "duration": "02:59",
  "year": "1977"
}
```

### Delete song metadata

`DELETE /songs?id=1,2`  
CSV of positive integer IDs (max 200 characters). Missing IDs are ignored.

Response `200 OK`:

```json
{ "ids": [1, 2] }
```

## Upload flow

1. Resource Service stores the MP3 in PostgreSQL.
2. Apache Tika (`Mp3Parser`) extracts ID3/XMP tags.
3. Only `xmpDM:duration` is transformed (seconds → `mm:ss` with leading zeros).
4. Tag values are mapped to song fields (`title` → `name`, etc.) without changing text.
5. Resource Service calls `POST /songs` on Song Service.

## Build

```bash
gradlew build
```
