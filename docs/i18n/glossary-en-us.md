# Glossary — en-US

Single source of truth for the English (United States) translation of the
Sports Management API. Use these terms consistently across code, API, database,
tests, and documentation.

## Decisions

| Topic | Decision |
|---|---|
| Scope | Source code, identifiers, messages, docs, HTTP contract, and database schema |
| HTTP contract | The existing `/v1` paths are renamed in place (breaking change) |
| Database | Renamed by Flyway `V22__rename_schema_to_english.sql`; applied migrations are never edited |
| Project identity | `sports-management-api`, package `br.gov.quixada.sports`, database `sports_management`, container `sports-management-mysql` |
| Locale-specific rules | Brazilian rules are preserved: CPF (`@CPF`, 11 digits), postal code `00000-000`, phone 10–12 digits, `America/Fortaleza` |
| Spelling | US English (`canceled`, `enrollment` is not used; we use `registration`) |

## Domain terms

| Portuguese | English |
|---|---|
| atleta | athlete |
| clube | club |
| equipe | team |
| modalidade | modality |
| categoria | category |
| competição | competition |
| inscrição de atleta | athlete registration |
| inscrição de equipe | team registration |
| partida | match |
| classificação | standings |
| rodada | round |
| mandante | home |
| visitante | away |
| local (da partida) | venue |
| número da camisa | jersey number |
| responsável | manager |
| observação | notes |
| data de cancelamento | canceled at |
| aproveitamento | points percentage |
| saldo de gols | goal difference |
| gols pró / contra | goals for / goals against |

## Attributes

| Portuguese | English |
|---|---|
| nome | name |
| nome completo | full name |
| nome completo normalizado | normalized full name |
| data de nascimento | birth date |
| data de cadastro | created at |
| data de início / fim | start date / end date |
| data de início / fim das inscrições | registration start date / registration end date |
| data e hora | scheduled at |
| idade / idade mínima / máxima | age / min age / max age |
| telefone | phone |
| endereço | address |
| logradouro | street |
| número | number |
| bairro | neighborhood |
| cidade | city |
| CEP | postal code |
| sexo | gender |
| ano | year |
| status | status |
| pontos | points |
| jogos | played |
| vitórias / empates / derrotas | wins / draws / losses |
| posição | position |

## Enumerations

| Portuguese | English |
|---|---|
| ATIVO / INATIVO | ACTIVE / INACTIVE |
| PLANEJAMENTO | PLANNING |
| INSCRICOES_ABERTAS | REGISTRATION_OPEN |
| INSCRICOES_ENCERRADAS | REGISTRATION_CLOSED |
| EM_ANDAMENTO | IN_PROGRESS |
| FINALIZADA | COMPLETED |
| CANCELADA | CANCELED |
| AGENDADA | SCHEDULED |
| PENDENTE | PENDING |
| APROVADA | APPROVED |
| REJEITADA | REJECTED |
| MASCULINO / FEMININO | MALE / FEMALE |

## HTTP paths

| Portuguese | English |
|---|---|
| `/v1/atletas` | `/v1/athletes` |
| `/v1/clubes` | `/v1/clubs` |
| `/v1/equipes` | `/v1/teams` |
| `/v1/modalidades` | `/v1/modalities` |
| `/v1/categorias` | `/v1/categories` |
| `/v1/competicoes` | `/v1/competitions` |
| `/v1/inscricoes-atletas` | `/v1/athlete-registrations` |
| `/v1/inscricoes-equipes` | `/v1/team-registrations` |
| `/v1/partidas` | `/v1/matches` |
| `/v1/competicoes/{id}/categorias/{catId}/classificacao` | `/v1/competitions/{id}/categories/{catId}/standings` |

## Database tables

| Portuguese | English |
|---|---|
| `db_atletas` | `db_athletes` |
| `db_clube` | `db_club` |
| `db_equipe` | `db_team` |
| `db_modalidade` | `db_modality` |
| `db_categoria` | `db_category` |
| `db_competicao` | `db_competition` |
| `db_inscricao_atleta` | `db_athlete_registration` |
| `db_inscricao_equipe` | `db_team_registration` |
| `db_partida` | `db_match` |
| `db_users` | `db_users` (already English) |
| `db_user_audit` | `db_user_audit` (already English) |

Column names follow the same word map, in `snake_case` (for example
`nome_completo` → `full_name`, `data_nascimento` → `birth_date`,
`id_competicao` → `competition_id`).

## Kept acronyms

`CPF`, `CEP`, `JWT`, `CORS`, `API`, `HTTP`, `JSON` keep their spelling. `CPF`
and `CEP` are Brazilian domain acronyms; their validation rules do not change.
