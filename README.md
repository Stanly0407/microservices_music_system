# Music Microservices

Spring Boot 3.4 / Java 21 / PostgreSQL 17 / Docker.

| Service | Port | Database |
|---------|------|----------|
| Resource Service | 8081 | resource-db:5432 |
| Song Service | 8082 | song-db:5432 |

---

## Quick Start

Docker (all services containerized):
docker compose up -d --build

Local development (DBs in Docker, services in IDE):
docker compose up -d resource-db song-db 

Then run ResourceServiceApplication and SongServiceApplication in IntelliJ

---

## API

### Resource Service (8081)

- POST /resources — upload MP3 (Content-Type: audio/mpeg) → {"id": 1}
- GET /resources/{id} — download MP3
- DELETE /resources?id=1,2,3 → {"ids": [1,2,3]}

### Song Service (8082)

- GET /songs/{id} → metadata (name, artist, album, duration, year)
- DELETE /songs?id=1,2,3 → {"ids": [1,2,3]}

Metadata is automatically extracted when MP3 is uploaded (Apache Tika).

---

## Configuration

- Services use localhost by default: localhost:5432, localhost:5433
- In Docker, values from .env are used (service names: resource-db, song-db)
- DBs initialized via SQL scripts (init-scripts/*/init.sql), data is ephemeral

---

## Commands

### Build locally
gradlew build

### Run service locally
gradlew :resource-service:bootRun

### Stop Docker containers
docker compose down

### View logs
docker compose logs -f resource-service

---

## Troubleshooting

Error `exit code: 127` → rebuild: docker compose down && docker compose up -d --build

Service can't connect to DB → wait 15 sec and restart: docker compose restart resource-service

Port in use → netstat -ano | findstr :8081 (Windows) or lsof -i :8081 (Mac/Linux)