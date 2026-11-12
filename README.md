# Sports Management API

REST API for managing sports competitions: athletes, clubs, teams, modalities, categories, competitions, registrations, matches and standings, with JWT authentication and role-based access control.

## Stack

- **Java 25** + **Spring Boot 4.1.0** (Maven Wrapper)
- **Spring Data JPA** + **MySQL 8.4** (dev) / **H2** (tests)
- **Flyway** as the source of truth for the schema
- **Spring Security + JWT**
- **MapStruct** + **Lombok**

## Requirements

- JDK 25 (`java -version`)
- Docker (for the development MySQL)
- No Maven installation required (use `./mvnw`)

## Configuration

1. Create the `.env` from the example and fill in the values:

   ```bash
   cp .env.example .env
   ```

   Required variables:

   | Variable | Description |
   |---|---|
   | `MYSQL_USER` | database user |
   | `MYSQL_PASSWORD` | database password |
   | `MYSQL_DATABASE` | database name |
   | `MYSQL_ROOT_PASSWORD` | container root password |
   | `API_SECURITY_TOKEN_SECRET` | JWT secret (minimum 32 characters) |
   | `API_CORS_ALLOWED_ORIGINS` | optional in dev; comma-separated front-end origins (default `http://localhost:3000,http://localhost:5173`) |

   > Never commit the `.env` or real secrets.

2. Start the development database:

   ```bash
   docker compose up -d
   ```

3. Start the application (`dev` profile by default):

   ```bash
   ./mvnw spring-boot:run
   ```

   The API runs at `http://localhost:8080`.

### Profiles

| Profile | File | Usage |
|---|---|---|
| `dev` (default) | `application-dev.yaml` | Local MySQL, Docker Compose, `show-sql: true`, `ddl-auto: validate` |
| `test` | `application-test.yaml` | In-memory H2, Flyway off, `create-drop` (enforced by Surefire) |
| `prod` | `application-prod.yaml` | `SPRING_PROFILES_ACTIVE=prod`; `show-sql: false`, Flyway on, `validate` |

In `prod`, boot **fails** if `API_SECURITY_TOKEN_SECRET`, `API_CORS_ALLOWED_ORIGINS`, `DB_URL`, `DB_USER` or `DB_PASSWORD` is missing. The schema is applied by Flyway; Hibernate only validates it.

### en-US migration (breaking)

This release translates identifiers, the HTTP contract, and the database schema.

- REST paths, JSON field names, and enum values changed. Update clients before deploying.
- Set `MYSQL_DATABASE=sports_management` in `.env` and use the new development URL.
- Flyway `V22__rename_schema_to_english.sql` renames tables, columns, indexes, and enum values in place. **Back up the database before running it**; rollback is done by restoring the backup.
- Historical migrations `V1`–`V21` were not modified. Their comments remain in Portuguese to preserve the Flyway checksum; the English schema starts at `V22`.

### CORS

Allowed origins come from `api.cors.allowed-origins` (`API_CORS_ALLOWED_ORIGINS`). A preflight from an unlisted origin gets `403`. Allowed methods: `GET, POST, PUT, PATCH, DELETE, OPTIONS`; headers: `Authorization`, `Content-Type`; `Location` is exposed.

## Tests

Tests use the `test` profile + in-memory H2 and **do not need MySQL/Docker**:

```bash
./mvnw test
./mvnw verify
```

## Authentication and authorization

- `POST /v1/auth/register` — creates a user with the `USER` role.
  ```json
  { "username": "john", "email": "john@example.com", "password": "strongPassword123" }
  ```
- `POST /v1/auth/login` — authenticates by email and returns the token.
  ```json
  { "email": "john@example.com", "password": "strongPassword123" }
  ```
- Send the token on protected routes: `Authorization: Bearer <token>`.

Roles:

| Role | Permissions |
|---|---|
| `USER` | `GET /v1/**` |
| `ADMIN` | write (`POST`/`PUT`/`PATCH`/`DELETE`) on `/v1/**`; read `/v1/users` |
| `SUPERADMIN` | user management; exclusive for administering `ADMIN`s |

There is exactly **one** `SUPERADMIN` (created by seed). The API does not allow creating or promoting another one. To recover this profile's password, see [docs/operacao/superadmin-recovery.md](docs/operacao/superadmin-recovery.md).

## API versioning

Native Spring versioning by the first path segment. All resources use `/vN/...` and declare `version = "1.0"`:

| Resource | Base |
|---|---|
| Athletes | `/v1/athletes` |
| Categories | `/v1/categories` |
| Clubs | `/v1/clubs` |
| Competitions | `/v1/competitions` |
| Teams | `/v1/teams` |
| Modalities | `/v1/modalities` |
| Athlete registrations | `/v1/athlete-registrations` |
| Team registrations | `/v1/team-registrations` |
| Matches | `/v1/matches` |
| Users | `/v1/users` |
| Standings | `/v1/competitions/{id}/categories/{catId}/standings` |

An unsupported version (e.g., `/v9/...`) responds with `400`. Paths outside the `/vN/...` pattern (e.g., `/error`) are not versioned.

Decision: versioning by **URL** (`/v1`), chosen for simplicity and browser compatibility. The header-based alternative (`Accept: application/vnd.sports.v1+json`) was rejected. Evolution: a future `/v2` must keep `/v1` available for a deprecation period before removal.

## Deletion and notes

- `DELETE /v1/athletes/{id}` is the API's **only physical deletion**. It requires the athlete to be `INACTIVE` and have no registrations that take a slot (`PENDING`/`APPROVED`/`INACTIVE`); `CANCELED`/`REJECTED` registrations are removed along with it. Responds `204`, `404` or `409`.
- The other resources have no physical deletion: disposal is logical, via `status` (`INACTIVE`/`CANCELED`).
- Competitions, team registrations, athlete registrations and matches have **`notes`** — free text (up to 500 characters), optional and **not tied to status**. It is used to communicate any information (cancellation reason, date extensions, announcements). It can be set on `POST`, and changed or cleared at any time via `PATCH .../notes` (body `{"notes":"..."}`; absent or blank clears the field).
- The same resources expose **`canceledAt`**, filled in automatically and only on the transition to `CANCELED` (informational).

## Operational documentation

- [SUPERADMIN password recovery](docs/operacao/superadmin-recovery.md) — server-side procedure (CLI).

## Security

- The `.env` is ignored by Git; keep secrets out of the repository.
- Boot fails if `API_SECURITY_TOKEN_SECRET` is not set (fail-fast).
- Change the `SUPERADMIN` password on the first deploy. The seed is public in the migration history; rotation is mandatory.

### Login rate limit

`POST /v1/auth/login` applies two in-memory limits per attempt: per IP (`max-per-ip`, default 100/15 min) and per (IP, email) (`max-per-ip-email`, default 5/15 min). When exceeded, it responds `429` with `Retry-After` (seconds) in the standard error format. A successful login gives the attempt back.

This is not an account lockout: there is no permanent lock and no state in the database. The limits live under `api.security.login-rate-limit` (in prod, `API_LOGIN_RATE_LIMIT_*`). The counter is per instance, so with N replicas the effective limit is N×; behind a reverse proxy, configure `server.forward-headers-strategy` so the real IP reaches `getRemoteAddr()`.
