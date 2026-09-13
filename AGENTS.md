# AGENTS.md

## Stack
- Java 25 + Spring Boot 4.1.0 + Maven wrapper (`./mvnw`, Maven 3.9.16). `pom.xml:16` pins `java.version=25` — ensure JDK 25 is active (`java -version`).
- Spring Data JPA + MySQL 8.4 (`mysql-connector-j` runtime). Hibernate `ddl-auto: update` — no Flyway/Liquibase despite `HELP.md` mentioning it; `src/main/resources/db/migration/` is empty.
- MapStruct 1.6.3 + Lombok. Both must stay in `pom.xml:89-99` `maven-compiler-plugin.annotationProcessorPaths` — removing either breaks generated mappers.

## Commands
```bash
cp .env.example .env  # fill MYSQL_USER, MYSQL_PASSWORD, MYSQL_DATABASE, MYSQL_ROOT_PASSWORD
docker compose up -d  # starts gestao-esportiva-mysql on 3306 (healthcheck 30s start_period)
./mvnw spring-boot:run          # requires Docker daemon — spring-boot-docker-compose auto-starts compose.yaml
./mvnw verify                   # full build
./mvnw test                     # needs running MySQL; contextLoads fails without DB/.env
./mvnw -Dtest=GestaoEsportivaApiApplicationTests#contextLoads test  # single test
./mvnw package -DskipTests
```

## Database / Env
- `.env` is gitignored. App reads `MYSQL_USER` / `MYSQL_PASSWORD` at `src/main/resources/application.yaml:7-8`; URL `jdbc:mysql://localhost:3306/gestao_esportiva?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=America/Recife` (`application.yaml:6`). Dev defaults in commited `.env`: `esporte/esporte/gestao_esportiva/root`.
- `spring-boot-docker-compose` (`pom.xml:66`) is `runtime` — `./mvnw spring-boot:run` manages `compose.yaml` lifecycle automatically. If Docker is down, run fails with connection refused rather than clear message.
- `application.yaml:12` `ddl-auto: update` and `show-sql: true` / `format_sql: true` are dev-only; do not rely on migrations.

## Architecture
- Entrypoint: `src/main/java/br/gov/quixada/esporte/GestaoEsportivaApiApplication.java:7`.
- Package `br.gov.quixada.esporte` — only `atleta` is fully wired (`Atleta.java:43` entity → `AtletaRepository.java:10` → `AtletaService.java:17` → `AtletaController.java:19` → `AtletaMapper.java:18`). Other domains (`equipe`, `clube`, `modalidade`, `competicao`, `categoria`, `inscricao`) are stub entities/repositories with no service/controller.
- Atleta REST base: `/v1/atletas` (`AtletaController.java:17`): `GET /?nome=`, `GET /{id}`, `POST /`, `PUT /{id}`, `PATCH /{id}/ativar`, `PATCH /{id}/inativar`. No pagination yet — `AtletaService.java:22` loads all rows.
- DTOs are `record`s in `src/main/java/br/gov/quixada/esporte/atleta/dto/`. Mapping via MapStruct `componentModel="spring"`.

## Gotchas — Won't Find Without Reading Code
- **CPF normalization**: `src/main/java/br/gov/quixada/esporte/extras/CpfUtils.java:8` strips non-digits. Called in `Atleta.java:84` `@PrePersist/@PreUpdate` AND `AtletaService.java:40` before `existsByCpf`. Entity has `@CPF` + `@Pattern("\\d{11}")` (`Atleta.java:54-55`) which rejects formatted CPF before normalization — see `src/main/resources/melhorias-atleta.md:42-61` for full analysis. Fix belongs in DTOs/service, not entity.
- **MapStruct + Lombok coupling**: Generated mappers go to `target/generated-sources/annotations/`. If you edit `AtletaMapper.java:42` `calcularIdade()` (`LocalDate.now()` not testable) run `./mvnw clean compile` to regenerate.
- **`@CreationTimestamp` Hibernate-specific** (`Atleta.java:78`) — tied to Hibernate, not Spring Data auditing. Tests need full context.
- **`GlobalExceptionHandler.java:11` only handles `AtletaNotFoundException` and `CpfJaCadastradoException`** — `MethodArgumentNotValidException` (failed `@Valid`) returns Spring default, not `ApiError`.
- **Active work notes**: `src/main/java/br/gov/quixada/esporte/iterations/melhorias-atleta.md` is the authoritative bug/roadmap checklist (12-section analysis). Check git branch `dev` is active development branch (`main` is stale) — current uncommitted changes on `dev` per `git status`.
- **No CI / lint / formatter config** in repo. No `opencode.json`. No `.github/` workflows. `HELP.md` is vanilla Spring Initializr boilerplate — ignore Flyway references there.
