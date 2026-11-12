-- =============================================================================
-- V22__rename_schema_to_english.sql
--
-- Translates the schema to en-US (United States). This migration only renames
-- tables, columns, indexes, and enum values; it does not change constraints or
-- business rules.
--
-- MySQL enum values cannot be renamed directly: the column is first extended
-- with both old and new values, the rows are updated, and the column is then
-- narrowed to the new values only.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. Tables
-- -----------------------------------------------------------------------------
rename table db_atletas to db_athletes;
rename table db_categoria to db_category;
rename table db_clube to db_club;
rename table db_competicao to db_competition;
rename table db_equipe to db_team;
rename table db_inscricao_atleta to db_athlete_registration;
rename table db_inscricao_equipe to db_team_registration;
rename table db_modalidade to db_modality;
rename table db_partida to db_match;

-- -----------------------------------------------------------------------------
-- 2. Columns
-- -----------------------------------------------------------------------------
alter table db_athletes
    change column data_nascimento birth_date date not null,
    change column data_cadastro created_at datetime(6) not null,
    change column cep postal_code varchar(10) not null,
    change column numero number varchar(10) not null,
    change column telefone phone varchar(20) not null,
    change column bairro neighborhood varchar(50) not null,
    change column cidade city varchar(50) not null,
    change column logradouro street varchar(150) not null,
    change column nome_completo full_name varchar(150) not null,
    change column nome_completo_normalizado normalized_full_name varchar(150) not null;

alter table db_category
    change column idade_maxima max_age integer not null,
    change column idade_minima min_age integer not null,
    change column id_competicao competition_id bigint not null,
    change column nome name varchar(100) not null;

alter table db_club
    change column cep postal_code varchar(10) not null,
    change column numero number varchar(10) not null,
    change column telefone phone varchar(20) not null,
    change column bairro neighborhood varchar(50) not null,
    change column cidade city varchar(50) not null,
    change column logradouro street varchar(150) not null,
    change column nome name varchar(100) not null,
    change column responsavel manager varchar(100) not null;

alter table db_competition
    change column ano competition_year integer not null,
    change column data_fim end_date date not null,
    change column data_inicio start_date date not null,
    change column data_inicio_inscricao registration_start_date date not null,
    change column data_fim_inscricao registration_end_date date not null,
    change column id_modalidade modality_id bigint not null,
    change column nome name varchar(100) not null,
    change column observacao notes varchar(500) null,
    change column data_cancelamento canceled_at datetime(6) null;

alter table db_team
    change column id_clube club_id bigint not null,
    change column id_modalidade modality_id bigint not null,
    change column nome name varchar(100) not null,
    change column responsavel manager varchar(100) not null;

alter table db_athlete_registration
    change column numero_camisa jersey_number varchar(3) not null,
    change column data_inscricao registration_date datetime(6) not null,
    change column id_atleta athlete_id bigint not null,
    change column id_inscricao_equipe team_registration_id bigint not null,
    change column observacao notes varchar(500) null,
    change column data_cancelamento canceled_at datetime(6) null;

alter table db_team_registration
    change column data_inscricao registration_date datetime(6) not null,
    change column id_categoria category_id bigint not null,
    change column id_competicao competition_id bigint not null,
    change column id_equipe team_id bigint not null,
    change column observacao notes varchar(500) null,
    change column data_cancelamento canceled_at datetime(6) null;

alter table db_modality
    change column nome name varchar(100) not null;

alter table db_match
    change column rodada round integer not null,
    change column data_hora scheduled_at datetime(6) not null,
    change column local venue varchar(150) null,
    change column gols_mandante home_goals integer null,
    change column gols_visitante away_goals integer null,
    change column id_inscricao_mandante home_registration_id bigint not null,
    change column id_inscricao_visitante away_registration_id bigint not null,
    change column observacao notes varchar(500) null,
    change column data_cancelamento canceled_at datetime(6) null;

-- -----------------------------------------------------------------------------
-- 3. Indexes and unique constraints
-- -----------------------------------------------------------------------------
alter table db_athletes
    rename index idx_atleta_nome_completo_normalizado to idx_athlete_normalized_full_name;

alter table db_athlete_registration
    rename index uk_inscricao_atleta_numero to uk_athlete_registration_jersey,
    rename index uk_inscricao_atleta_atleta to uk_athlete_registration_athlete;

-- -----------------------------------------------------------------------------
-- 4. Enum columns
-- -----------------------------------------------------------------------------

-- athlete.gender
alter table db_athletes
    modify column gender enum('FEMININO','MASCULINO','FEMALE','MALE') not null;
update db_athletes
set gender = case gender when 'MASCULINO' then 'MALE' else 'FEMALE' end;
alter table db_athletes
    modify column gender enum('FEMALE','MALE') not null;

-- athlete.status
alter table db_athletes
    modify column status enum('ATIVO','INATIVO','ACTIVE','INACTIVE') not null;
update db_athletes
set status = case status when 'ATIVO' then 'ACTIVE' else 'INACTIVE' end;
alter table db_athletes
    modify column status enum('ACTIVE','INACTIVE') not null;

-- club.status
alter table db_club
    modify column status enum('ATIVO','INATIVO','ACTIVE','INACTIVE') not null default 'ATIVO';
update db_club
set status = case status when 'ATIVO' then 'ACTIVE' else 'INACTIVE' end;
alter table db_club
    modify column status enum('ACTIVE','INACTIVE') not null default 'ACTIVE';

-- category.status
alter table db_category
    modify column status enum('ATIVO','INATIVO','ACTIVE','INACTIVE') not null default 'ATIVO';
update db_category
set status = case status when 'ATIVO' then 'ACTIVE' else 'INACTIVE' end;
alter table db_category
    modify column status enum('ACTIVE','INACTIVE') not null default 'ACTIVE';

-- team.status
alter table db_team
    modify column status enum('ATIVO','INATIVO','ACTIVE','INACTIVE') not null default 'ATIVO';
update db_team
set status = case status when 'ATIVO' then 'ACTIVE' else 'INACTIVE' end;
alter table db_team
    modify column status enum('ACTIVE','INACTIVE') not null default 'ACTIVE';

-- modality.status
alter table db_modality
    modify column status enum('ATIVO','INATIVO','ACTIVE','INACTIVE') not null default 'ATIVO';
update db_modality
set status = case status when 'ATIVO' then 'ACTIVE' else 'INACTIVE' end;
alter table db_modality
    modify column status enum('ACTIVE','INACTIVE') not null default 'ACTIVE';

-- competition.status
alter table db_competition
    modify column status enum(
        'PLANEJAMENTO','INSCRICOES_ABERTAS','INSCRICOES_ENCERRADAS',
        'EM_ANDAMENTO','FINALIZADA','CANCELADA',
        'PLANNING','REGISTRATION_OPEN','REGISTRATION_CLOSED',
        'IN_PROGRESS','COMPLETED','CANCELED'
    ) not null;
update db_competition
set status = case status
    when 'PLANEJAMENTO' then 'PLANNING'
    when 'INSCRICOES_ABERTAS' then 'REGISTRATION_OPEN'
    when 'INSCRICOES_ENCERRADAS' then 'REGISTRATION_CLOSED'
    when 'EM_ANDAMENTO' then 'IN_PROGRESS'
    when 'FINALIZADA' then 'COMPLETED'
    else 'CANCELED'
end;
alter table db_competition
    modify column status enum(
        'PLANNING','REGISTRATION_OPEN','REGISTRATION_CLOSED',
        'IN_PROGRESS','COMPLETED','CANCELED'
    ) not null;

-- team registration.status
alter table db_team_registration
    modify column status enum(
        'PENDENTE','APROVADA','REJEITADA','CANCELADA','INATIVA',
        'PENDING','APPROVED','REJECTED','CANCELED','INACTIVE'
    ) not null;
update db_team_registration
set status = case status
    when 'PENDENTE' then 'PENDING'
    when 'APROVADA' then 'APPROVED'
    when 'REJEITADA' then 'REJECTED'
    when 'CANCELADA' then 'CANCELED'
    else 'INACTIVE'
end;
alter table db_team_registration
    modify column status enum('PENDING','APPROVED','REJECTED','CANCELED','INACTIVE') not null;

-- athlete registration.status
alter table db_athlete_registration
    modify column status enum(
        'PENDENTE','APROVADA','REJEITADA','CANCELADA','INATIVA',
        'PENDING','APPROVED','REJECTED','CANCELED','INACTIVE'
    ) not null;
update db_athlete_registration
set status = case status
    when 'PENDENTE' then 'PENDING'
    when 'APROVADA' then 'APPROVED'
    when 'REJEITADA' then 'REJECTED'
    when 'CANCELADA' then 'CANCELED'
    else 'INACTIVE'
end;
alter table db_athlete_registration
    modify column status enum('PENDING','APPROVED','REJECTED','CANCELED','INACTIVE') not null;

-- match.status
alter table db_match
    modify column status enum(
        'AGENDADA','EM_ANDAMENTO','FINALIZADA','CANCELADA',
        'SCHEDULED','IN_PROGRESS','COMPLETED','CANCELED'
    ) not null;
update db_match
set status = case status
    when 'AGENDADA' then 'SCHEDULED'
    when 'EM_ANDAMENTO' then 'IN_PROGRESS'
    when 'FINALIZADA' then 'COMPLETED'
    else 'CANCELED'
end;
alter table db_match
    modify column status enum('SCHEDULED','IN_PROGRESS','COMPLETED','CANCELED') not null;

-- user.status
alter table db_users
    modify column status enum('ATIVO','INATIVO','ACTIVE','INACTIVE') not null default 'ATIVO';
update db_users
set status = case status when 'ATIVO' then 'ACTIVE' else 'INACTIVE' end;
alter table db_users
    modify column status enum('ACTIVE','INACTIVE') not null default 'ACTIVE';
