# Pending Items — Sports Management API

> File consolidated on 2026-11-09; updated on 2026-11-12 to mark completed items. It gathers **everything that has not yet been implemented or is pending** in the project, extracted from `decisions.md`, `studies.md`, `fixes.md`, `future-iterations.md` and `securityfixes.md` (removed after consolidation).
>
> Completed items remain marked with `[x]` for traceability; pending ones stay `[ ]`.
>
> Priorities: **P0** critical/exploitable · **P1** important · **P2** improvement/good practice.

---

## Checklist

### Security / Authentication
- [x] **A5** — Rate limiting on login (bucket4j, no lockout) (P0)
- [ ] **C6** — Refresh token / logout / revocation (P2)
- [ ] **C7** — (Optional) roles as a JWT claim (P2)
- [x] **C8** — Configure CORS (P2)

### Users / Governance
- [x] Decision: exactly **1 SUPERADMIN**, no creation/promotion through the API (existing blocks in `UserService`)
- [x] SUPERADMIN password recovery via CLI (P1)
- [ ] Audit: filters by actor/target/period, retention/purge and export (P2)

### Athlete
- [x] `hardDelete` + `DELETE /v1/athletes/{id}` endpoint (P1)
- [ ] `AthleteControllerTest` (P1)
- [x] `AthleteMapperTest` (P1)
- [x] Configure standalone `MockMvc` + fixed `Clock` in controller tests (P1)

### Tests for the new flows
- [ ] Controller/mapper tests for competition, registration, match and standings (P1)

### Sports core
- [ ] Automatic round generation (single round-robin / double round-robin) (P1)
- [ ] Minimum/maximum roster size — decision with the technical staff (P1)

### Infrastructure / Documentation
- [ ] OpenAPI/springdoc + `@Tag`/`@Operation` (P2)
- [x] README + versioning strategy documentation (P2)
- [x] `application-prod.yaml` + separate dev/prod config (P2)
- [x] Verify Java 25 + Spring Boot 4.1.0 + MapStruct 1.6.3 compatibility (P2)
- [x] CI (GitHub Actions) running `./mvnw verify` (P2)
- [ ] Testcontainers (optional) (P2)

---

## Details

### A5 — Rate limiting on login (P0) — Completed
- **Where:** `security/auth/AuthController#login`, `security/auth/LoginRateLimiter`, `security/config/LoginRateLimitProperties`, `security/config/ClockTimeMeter`, `security/config/RateLimitConfig`, `security/auth/exception/TooManyAttemptsException`, `common/error/GlobalExceptionHandler`.
- **Previous situation:** unlimited attempts; `UserDetailsAdapter.isAccountNonLocked()` always returned `true`; there was no `bucket4j`/`Resilience4j` in the `pom.xml`.
- **Decision:** **rate limiting, not account lockout**, with **bucket4j core** (`bucket4j_jdk17-core:8.22.0`, explicit version) + **Caffeine** (version managed by Boot) to bound memory. No migration and no persistent state. `Resilience4j` stays reserved for future external calls (retry/circuit breaker).
- **Model:** two in-memory buckets per attempt — per **IP** (`max-per-ip`) and per **(IP, normalized email)** (`max-per-ip-email`). Never by email alone: it would allow locking out third parties' accounts (DoS). Consumption happens **before** BCrypt; a successful login returns the attempt (`addTokens`). When exceeded → `429` in the `ApiError` format + `Retry-After` header (seconds, from `getNanosToWaitForRefill`), with a generic message.
- **Config:** `api.security.login-rate-limit.{max-per-ip:100, max-per-ip-email:5, window:15m, max-keys:100000}`. In prod, tunable via `API_LOGIN_RATE_LIMIT_*` (with defaults).
- **Semantics:** `refillGreedy` ⇒ 5 attempts/15 min = 1 token every 3 min. It is soft throttling, not a hard 15-minute block.
- **Limitations:** state is in memory and per instance — N replicas ⇒ effective limit N×; a restart resets counters; Caffeine eviction (`max-keys`) recreates full buckets (fail-open); the evolution is to swap the `Cache<String,Bucket>` for a `ProxyManager` (Redis/MySQL). The IP comes from `request.getRemoteAddr()`; behind a trusted proxy, configure `server.forward-headers-strategy` (otherwise all traffic appears to come from the proxy IP).
- **Tests:** `LoginRateLimiterTest` (8), `AuthControllerLoginRateLimitTest` (4) and `LoginRateLimitPropertiesTest` (3), with no `Thread.sleep` (`ClockTimeMeter` + `MutableClock`; IP via `request.setRemoteAddr`).

### C6 — Refresh token / logout / revocation (P2)
- **Where:** the auth flow as a whole (`security/jwt/JwtService`, `security/auth/AuthController`).
- **Situation:** the token lasts `2h` (`api.security.token.expiration`) and there is no refresh or server-side logout.
- **Why:** when it expires, the user has to log in again; there is no way to invalidate a token before expiry (e.g. password change, logout). Plain JWT does not solve this.
- **How to implement:** define the long-term session strategy — refresh token + blacklist/allowlist. The `security/jwt` package was split precisely to prepare for this.
- **How to test:** revoke a token and verify it starts answering `401`; refresh issues a new access token.

### C7 — (Optional) roles as a JWT claim (P2)
- **Where:** `security/jwt/JwtService` + `security/config/JwtAuthenticationFilter`.
- **Situation:** the filter performs **one SELECT against the database per request** to load the user and their authorities.
- **Trade-off:** putting the roles in the token avoids the query, but loses immediate revocation (an old token keeps the old role until it expires). Performance × revocation — decide.

### C8 — Configure CORS (P2) — Completed
- **Where:** `security/config/CorsProperties`, `SecurityConfig`, `application.yaml` / `application-prod.yaml`.
- **How it turned out:** `api.cors.allowed-origins` (list, via `API_CORS_ALLOWED_ORIGINS`) → `CorsConfigurationSource` bean registered on `/**` and applied by `.cors(...)` in the `SecurityFilterChain`. Methods `GET/POST/PUT/PATCH/DELETE/OPTIONS`; headers `Authorization` and `Content-Type`; `Location` exposed; `maxAge` 1h.
- **Behavior:** preflight from a non-listed origin → `403`; listed origin → `200` with `Access-Control-Allow-Origin`.
- **Tests:** `CorsConfigTest` (`@SpringBootTest` + `@AutoConfigureMockMvc`) covers both an allowed origin and an intruder origin.
- **Attention in prod:** the boot fails if `API_CORS_ALLOWED_ORIGINS` is not provided.

### SUPERADMIN password recovery via CLI (P1) — Completed
- **Status:** implemented in commit `f97544d`. Runbook at `docs/operacao/superadmin-recovery.md`.
- **Decision:** the system keeps **exactly 1 SUPERADMIN**, responsible for managing the `ADMIN` users. The API still blocks creating/promoting a `SUPERADMIN` (`UserService`) and there is no recovery endpoint exposed on the network.
- **Where:** `security/users/SuperadminPasswordResetService`, `security/users/cli/SuperadminPasswordResetRunner`, profile `superadmin-reset` (`application-superadmin-reset.yaml`).
- **Why:** with no email/MFA, the real risk is losing the password of the single SUPERADMIN. Recovery requires shell access on the server — the same privilege level that would already allow changing the database — so it adds no remote attack surface.
- **How it works:** `java -jar app.jar --spring.profiles.active=prod,superadmin-reset` (or `dev,superadmin-reset`); the runner reads the email (`--reset.email`/`SUPERADMIN_EMAIL`, default `admin@quixada.gov.br`) and the password (interactive console or `SUPERADMIN_NEW_PASSWORD`), re-hashes with BCrypt, reactivates the account and audits `PASSWORD_RESET`, then exits.
- **Limitation:** JWT is stateless — tokens issued before the reset remain valid until they expire (2h); immediate revocation depends on item C6.
- **How to test:** `SuperadminPasswordResetServiceTest` covers success, non-existent email, non-SUPERADMIN user and out-of-range password.

### User audit: filters, retention/purge and export (P2)
- **Where:** `security/users/UserController#findAudit` (`GET /v1/users/audit`), `security/users/UserAuditService#findAll`, `UserAuditRepository`, table `db_user_audit` (`V18`).
- **Situation:** only `findAll(Pageable)` exists — no filters and no export; there is no retention/purge policy.
- **How to implement:** filters by actor/target/action and date range (Specifications), an export endpoint (CSV/JSON) and a retention routine/job.
- **How to test:** `GET /v1/users/audit?actorEmail=...&from=...&to=...`; the purge removes records outside the window.

### Athlete `hardDelete` + endpoint (P1) — Completed
- **Status:** implemented. `AthleteService#hardDelete` + `DELETE /v1/athletes/{id}`.
- **Team decision:** physical deletion **only for athletes**. The other domains are not deleted: they use `status` (`INACTIVE`/`CANCELED`) plus a free-text `notes` field to record any information (cancellation reason, date extension, announcements).
- **Rule:** requires the athlete to be `INACTIVE` (otherwise `409`) and no registration in `OCCUPY_SLOT` (`PENDING`/`APPROVED`/`INACTIVE`). Registrations in `FREE_SLOT` (`CANCELED`/`REJECTED`) are deleted along with it. Returns `204`; `404` if it does not exist.
- **Verified impact:** `db_athletes` is only referenced by `db_athlete_registration`; standings do not use athlete registrations and there is no athlete audit. Accepted side effect: the CPF unique constraint is released.
- **Tests:** `AthleteServiceTest` (nested `hardDelete`) and `AthleteHardDeleteRepositoryTest` (the FK blocks the delete when a registration exists; `deleteByAthleteIdAndStatusIn` frees the path).

### `notes` field + `canceled_at` (V21) — Completed
- **Team decision:** instead of physically deleting competition/registration/match, record the reason/context in a free-text field.
- **`notes`:** free text (`varchar(500)`, optional) on `Competition`, `TeamRegistration`, `AthleteRegistration` and `Match`. **Not tied to status** — it can be set on `POST`, edited or cleared at any time via `PATCH /v1/{resource}/{id}/notes` (the `*NotesRequest` records), including on `CANCELED`/`COMPLETED`/`INACTIVE` records. Absent/blank ⇒ `null`. It is not propagated in cascade.
- **`canceledAt`:** informational, written only on the transition to `CANCELED` (including the bulk update of matches).
- **Not in listings:** the `*SummaryResponse` records do not expose `notes`.

### `AthleteControllerTest` (P1)
- **Where:** `src/test/java/.../athlete/AthleteControllerTest.java` (does not exist yet).
- **Minimum scenarios:** valid `POST /v1/athletes` → `201` + `Location` header; invalid `POST` → `400` with `errors[].field`; `PATCH /{id}/status` → `200`; `GET /v1/athletes/-1` → `400`; `PUT /{id}` on an inactive athlete → `409`.
- **Attention:** the versioned controller requires `ApiVersionStrategy` in standalone MockMvc (see `AuthControllerTest`), otherwise it throws `IllegalStateException`.

### `AthleteMapperTest` (P1) — Completed
- **Where:** `src/test/java/.../athlete/AthleteMapperTest.java`.
- **How it turned out:** instantiates `AthleteMapperImpl` directly and injects `AddressMapperImpl` via `ReflectionTestUtils` (the generated impl uses field injection). 9 cases: `calculateAge` with `Clock.fixed` (including the day before the birthday and null dates); `toEntity(AthleteUpdateRequest)` does not map `cpf`, `id`, `createdAt` nor `status`; `toGetResponse`/`toSummary` calculate the age and return `null` when `birthDate` is null.

### Configure standalone `MockMvc` + fixed `Clock` in controller tests (P1) — Completed
- **Where:** `src/test/java/br/gov/quixada/sports/support/ControllerTestSupport.java`, `config/ClockTestConfig.java`.
- **How it turned out:** `ControllerTestSupport` centralizes the standalone MockMvc (with `GlobalExceptionHandler` + `ApiVersionStrategy`) and exposes `FIXED_CLOCK` (`2026-06-15T12:00:00Z`, `America/Fortaleza`). `ClockTestConfig` is a `@TestConfiguration` with `@Primary` to override the `Clock` bean in tests that boot the context (`@Import(ClockTestConfig.class)`).
- **Adoption:** `AuthControllerTest` and `CompetitionControllerTest` now use `ControllerTestSupport.mockMvc(...)`, removing the duplicated versioning strategy. `AthleteControllerClockTest` validates the age calculated with the fixed clock on `GET` and `POST`; `ClockTestConfigTest` guarantees the fixed bean takes precedence.

### Controller/mapper tests for the new flows (P1)
- **Where:** `src/test/java/.../` — currently there are `AuthControllerTest`, `CompetitionControllerTest` (notes) and `AthleteMapperTest`.
- **Missing scope:** competition, registration (`AthleteRegistration`/`TeamRegistration`), match and standings — controllers and mappers.

### Automatic round generation (single round-robin / double round-robin) (P1)
- **Where:** `match/` (`Match.round`, `MatchCreateRequest.round`).
- **Situation:** `round` is a manually informed field; there is no automatic schedule generation.
- **How to implement:** generate matchups from the competition/category registrations in `APPROVED` status (round-robin, single/double round-robin), respecting the "one team per round/time slot" rule.
- **How to test:** N teams → expected number/structure of matches; the double round-robin swaps home/away.

### Minimum/maximum roster size (P1)
- **Where:** `team`/`registration` rules.
- **Situation:** there is no validation of the minimum/maximum roster size.
- **Blocker:** depends on a decision with the technical staff about the limits per modality/category.
- **How to implement:** once defined, validate in `TeamRegistrationService` and cover with tests.

### OpenAPI/springdoc + annotations (P2)
- **Where:** `pom.xml` (no `springdoc-openapi-starter-webmvc-ui`), controllers.
- **How to implement:** add the dependency, annotate controllers with `@Tag`/`@Operation`, expose `swagger-ui.html` and validate `GET /v1/athletes?name=&page=0` and `POST` with `Location`.
- **Attention:** `spring.jpa.open-in-view=false` is already on — map DTOs inside `@Transactional` or use `@EntityGraph`/`JOIN FETCH`.

### README + versioning strategy documentation (P2) — Completed
- **Status:** `README.md` created, with setup, authentication/roles, endpoint table, tests and the versioning decision (URL `/v1`; header-based alternative discarded; deprecation policy).
- **Note:** native versioning was already implemented (`/v{version}/...`, `version = "1.0"`); only the documentation was missing, now published.

### `application-prod.yaml` + separate dev/prod config (P2) — Completed
- **How it turned out:** `application.yaml` no longer enables `show-sql`/`format_sql` (now only in `dev`); `application-dev.yaml` gained `show-sql: true`, `format_sql` and `spring.docker.compose.enabled: true`; `application-prod.yaml` has `show-sql: false`, `format_sql: false`, `ddl-auto: validate`, Flyway on, Docker Compose off, configurable `server.port` and log levels.
- **Variables required in prod:** `DB_URL`, `DB_USER`, `DB_PASSWORD`, `API_CORS_ALLOWED_ORIGINS`, `API_SECURITY_TOKEN_SECRET` (the boot fails without them).

### Verify Java 25 + Boot 4.1.0 + MapStruct 1.6.3 compatibility (P2) — Completed
- **Result:** `./mvnw clean compile` with JDK 25.0.4.1 generates all `*MapperImpl` normally. The check **found and fixed** two `Unmapped target property: "normalizedFullName"` warnings in `AthleteMapper` (added `@Mapping(ignore = true)` on both `toEntity` methods). The build no longer emits any `[WARNING]` from the compiler.
- **Remaining issue:** JDK 25 prints the `sun.misc.Unsafe::objectFieldOffset ... terminally deprecated` warning triggered by `lombok.permit.Permit` (Lombok 1.18.46, already the latest version). It is harmless and has no fix on the project side; track it until Lombok ships an implementation without `Unsafe`.

### CI (GitHub Actions) (P2) — Completed
- **Where:** `.github/workflows/ci.yml`.
- **How it turned out:** workflow triggered on `push`/`pull_request` to `dev` and `main`, with JDK 25 (Temurin), Maven cache and `./mvnw --batch-mode --no-transfer-progress verify` (profile `test`/H2, no MySQL). `permissions: contents: read` and per-ref `concurrency`.

### Testcontainers (optional) (P2)
- **Where:** integration tests.
- **How to implement:** spin up a real MySQL in a container for tests that depend on MySQL dialect/constraints (today the tests use H2 — the `errorCode == 1062` CPF branch is not exercised).

---

## Blockers / decisions required before implementing
- **Minimum/maximum roster size:** depends on the technical staff (limits per modality/category).
- **`SUPERADMIN` creation through the API:** decided — exactly 1 SUPERADMIN, no creation/promotion endpoint; password recovery via CLI.
- **C6/C7:** define the trade-off between immediate revocation and performance (SELECT per request × roles in the token).
