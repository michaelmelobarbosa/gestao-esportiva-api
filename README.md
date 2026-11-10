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

## Documentação operacional

- [Recuperação de senha do SUPERADMIN](docs/operacao/recuperacao-superadmin.md) — procedimento de servidor (CLI).

## Segurança

- O `.env` é ignorado pelo Git; mantenha segredos fora do repositório.
- O boot falha se `API_SECURITY_TOKEN_SECRET` não estiver definido (fail-fast).
- Troque a senha do `SUPERADMIN` no primeiro deploy. O seed é público no histórico de migrations; a rotação é obrigatória.
