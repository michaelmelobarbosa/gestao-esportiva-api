# Gestão Esportiva API

API REST para gestão de competições esportivas: atletas, clubes, equipes, modalidades, categorias, competições, inscrições, partidas e classificação, com autenticação JWT e controle de acesso por papéis.

## Stack

- **Java 25** + **Spring Boot 4.1.0** (Maven Wrapper)
- **Spring Data JPA** + **MySQL 8.4** (dev) / **H2** (testes)
- **Flyway** como fonte de verdade do schema
- **Spring Security + JWT**
- **MapStruct** + **Lombok**

## Requisitos

- JDK 25 (`java -version`)
- Docker (para o MySQL de desenvolvimento)
- Nenhuma instalação de Maven (use `./mvnw`)

## Configuração

1. Crie o `.env` a partir do exemplo e preencha os valores:

   ```bash
   cp .env.example .env
   ```

   Variáveis necessárias:

   | Variável | Descrição |
   |---|---|
   | `MYSQL_USER` | usuário do banco |
   | `MYSQL_PASSWORD` | senha do banco |
   | `MYSQL_DATABASE` | nome do banco |
   | `MYSQL_ROOT_PASSWORD` | senha do root do container |
   | `API_SECURITY_TOKEN_SECRET` | segredo do JWT (mínimo 32 caracteres) |
   | `API_CORS_ALLOWED_ORIGINS` | opcional em dev; origens do front-end separadas por vírgula (default `http://localhost:3000,http://localhost:5173`) |

   > Nunca comite o `.env` nem segredos reais.

2. Suba o banco de desenvolvimento:

   ```bash
   docker compose up -d
   ```

3. Suba a aplicação (profile `dev` por padrão):

   ```bash
   ./mvnw spring-boot:run
   ```

   A API fica em `http://localhost:8080`.

### Profiles

| Profile | Arquivo | Uso |
|---|---|---|
| `dev` (default) | `application-dev.yaml` | MySQL local, Docker Compose, `show-sql: true`, `ddl-auto: validate` |
| `test` | `application-test.yaml` | H2 em memória, Flyway off, `create-drop` (forçado pelo Surefire) |
| `prod` | `application-prod.yaml` | `SPRING_PROFILES_ACTIVE=prod`; `show-sql: false`, Flyway on, `validate` |

Em `prod` o boot **falha** se faltarem `API_SECURITY_TOKEN_SECRET`, `API_CORS_ALLOWED_ORIGINS`, `DB_URL`, `DB_USER` ou `DB_PASSWORD`. O schema é aplicado pelo Flyway; o Hibernate apenas valida.

### CORS

As origens permitidas vêm de `api.cors.allowed-origins` (`API_CORS_ALLOWED_ORIGINS`). Preflight de origem não listada recebe `403`. Métodos permitidos: `GET, POST, PUT, PATCH, DELETE, OPTIONS`; headers: `Authorization`, `Content-Type`; `Location` é exposto.

## Testes

Os testes usam o profile `test` + H2 em memória e **não precisam de MySQL/Docker**:

```bash
./mvnw test
./mvnw verify
```

## Autenticação e autorização

- `POST /v1/auth/register` — cria usuário com papel `USER`.
  ```json
  { "username": "joao", "email": "joao@example.com", "password": "senhaForte123" }
  ```
- `POST /v1/auth/login` — autentica por e-mail e devolve o token.
  ```json
  { "email": "joao@example.com", "password": "senhaForte123" }
  ```
- Envie o token nas rotas protegidas: `Authorization: Bearer <token>`.

Papéis:

| Papel | Permissões |
|---|---|
| `USER` | `GET /v1/**` |
| `ADMIN` | escrita (`POST`/`PUT`/`PATCH`/`DELETE`) em `/v1/**`; leitura de `/v1/users` |
| `SUPERADMIN` | gestão de usuários; exclusivo para administrar `ADMIN`s |

Existe exatamente **um** `SUPERADMIN` (criado por seed). A API não permite criar nem promover outro. Para recuperar a senha desse perfil, veja [docs/operacao/recuperacao-superadmin.md](docs/operacao/recuperacao-superadmin.md).

## Versionamento da API

Versionamento nativo do Spring pelo primeiro segmento do path. Todos os recursos usam `/vN/...` e declaram `version = "1.0"`:

| Recurso | Base |
|---|---|
| Atletas | `/v1/atletas` |
| Categorias | `/v1/categorias` |
| Clubes | `/v1/clubes` |
| Competições | `/v1/competicoes` |
| Equipes | `/v1/equipes` |
| Modalidades | `/v1/modalidades` |
| Inscrições de atletas | `/v1/inscricoes-atletas` |
| Inscrições de equipes | `/v1/inscricoes-equipes` |
| Partidas | `/v1/partidas` |
| Usuários | `/v1/users` |
| Classificação | `/v1/competicoes/{id}/categorias/{catId}/classificacao` |

Versão não suportada (ex.: `/v9/...`) responde `400`. Paths fora do padrão `/vN/...` (ex.: `/error`) não são versionados.

Decisão: versionamento por **URL** (`/v1`), escolhido por simplicidade e compatibilidade com navegador. A alternativa por header (`Accept: application/vnd.gestao.v1+json`) foi descartada. Evolução: uma eventual `/v2` deve manter `/v1` disponível por um período de deprecação antes da remoção.

## Exclusão e anotações

- `DELETE /v1/atletas/{id}` é a **única exclusão física** da API. Exige atleta `INATIVO` e sem inscrições que ocupem vaga (`PENDENTE`/`APROVADA`/`INATIVA`); inscrições `CANCELADA`/`REJEITADA` são removidas junto. Responde `204`, `404` ou `409`.
- Os demais recursos não têm exclusão física: o descarte é lógico, por `status` (`INATIVO`/`CANCELADA`).
- Competições, inscrições de equipe, inscrições de atleta e partidas possuem **`observacao`** — texto livre (até 500 caracteres), opcional e **sem vínculo com o status**. Serve para comunicar qualquer informação (motivo de cancelamento, prorrogação de datas, avisos). Pode ser criada no `POST`, alterada ou limpa a qualquer momento via `PATCH .../observacao` (corpo `{"observacao":"..."}`; ausente ou em branco limpa o campo).
- Os mesmos recursos expõem **`dataCancelamento`**, preenchida automaticamente e apenas na transição para `CANCELADA` (informativa).

## Documentação operacional

- [Recuperação de senha do SUPERADMIN](docs/operacao/recuperacao-superadmin.md) — procedimento de servidor (CLI).

## Segurança

- O `.env` é ignorado pelo Git; mantenha segredos fora do repositório.
- O boot falha se `API_SECURITY_TOKEN_SECRET` não estiver definido (fail-fast).
- Troque a senha do `SUPERADMIN` no primeiro deploy. O seed é público no histórico de migrations; a rotação é obrigatória.

### Rate limit de login

`POST /v1/auth/login` aplica dois limites em memória por tentativa: por IP (`max-por-ip`, default 100/15 min) e por (IP, e-mail) (`max-por-ip-email`, default 5/15 min). Ao exceder, responde `429` com `Retry-After` (segundos) no formato de erro padrão. Login bem-sucedido devolve a tentativa.

Não é lockout de conta: não há bloqueio permanente nem estado no banco. Os limites ficam em `api.security.login-rate-limit` (em prod, `API_LOGIN_RATE_LIMIT_*`). O contador é por instância, então com N réplicas o limite efetivo é N×; atrás de proxy reverso, configure `server.forward-headers-strategy` para o IP real chegar em `getRemoteAddr()`.
