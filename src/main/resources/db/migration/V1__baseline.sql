-- =============================================================================
-- V1__baseline.sql
-- Baseline do schema do gestao-esportiva-api, gerado a partir das entidades JPA
-- (spring.jpa.properties.jakarta.persistence.schema-generation.scripts).
--
-- A partir daqui o schema é versionado pelo Flyway:
--   - NUNCA editar esta migration depois de aplicada (o checksum quebra).
--   - Toda mudança de schema vira uma nova migration: V2__, V3__, ...
-- =============================================================================

create table db_atletas (
    data_nascimento date not null,
    data_cadastro datetime(6) not null,
    id bigint not null auto_increment,
    cep varchar(10) not null,
    numero varchar(10) not null,
    cpf varchar(11) not null,
    telefone varchar(20) not null,
    bairro varchar(50) not null,
    cidade varchar(50) not null,
    logradouro varchar(150) not null,
    nome_completo varchar(150) not null,
    sexo enum ('FEMININO','MASCULINO') not null,
    status enum ('ATIVO','INATIVO') not null,
    primary key (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table db_categoria (
    idade_maxima integer not null,
    idade_minima integer not null,
    id bigint not null auto_increment,
    id_competicao bigint not null,
    nome varchar(100) not null,
    primary key (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table db_clube (
    ativo BOOLEAN DEFAULT TRUE not null,
    id bigint not null auto_increment,
    cep varchar(10) not null,
    numero varchar(10) not null,
    telefone varchar(20) not null,
    bairro varchar(50) not null,
    cidade varchar(50) not null,
    nome varchar(100) not null,
    responsavel varchar(100) not null,
    logradouro varchar(150) not null,
    primary key (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table db_competicao (
    ano integer not null,
    data_fim date not null,
    data_inicio date not null,
    id bigint not null auto_increment,
    id_modalidade bigint not null,
    nome varchar(100) not null,
    status enum ('CANCELADA','EM_ANDAMENTO','FINALIZADA','INSCRICOES_ABERTAS','INSCRICOES_ENCERRADAS','PLANEJAMENTO') not null,
    primary key (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table db_equipe (
    ativo BOOLEAN DEFAULT TRUE not null,
    id bigint not null auto_increment,
    id_clube bigint not null,
    id_modalidade bigint not null,
    nome varchar(100) not null,
    responsavel varchar(100) not null,
    primary key (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table db_inscricao_atleta (
    numero_camisa varchar(3) not null,
    data_inscricao datetime(6) not null,
    id bigint not null auto_increment,
    id_atleta bigint not null,
    id_inscricao_equipe bigint not null,
    status enum ('APROVADA','CANCELADA','INATIVA','PENDENTE','REJEITADA') not null,
    primary key (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table db_inscricao_equipe (
    data_inscricao datetime(6) not null,
    id bigint not null auto_increment,
    id_categoria bigint not null,
    id_competicao bigint not null,
    id_equipe bigint not null,
    status enum ('APROVADA','CANCELADA','INATIVA','PENDENTE','REJEITADA') not null,
    primary key (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table db_modalidade (
    ativo BOOLEAN DEFAULT TRUE not null,
    id bigint not null auto_increment,
    nome varchar(100) not null,
    primary key (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

alter table db_atletas
   add constraint UK8bidaw0oly0m87yc6bqvtbhru unique (cpf);

alter table db_categoria
   add constraint FKq2d3lejaeisyor9hh8mp1m04m
   foreign key (id_competicao)
   references db_competicao (id);

alter table db_competicao
   add constraint FK75xmaw6jswc7l7uuf2rifmxcv
   foreign key (id_modalidade)
   references db_modalidade (id);

alter table db_equipe
   add constraint FKlf0o5xhogqywyehkeyjks78lc
   foreign key (id_clube)
   references db_clube (id);

alter table db_equipe
   add constraint FKj74sv4nots8j4u7jqh7f14iva
   foreign key (id_modalidade)
   references db_modalidade (id);

alter table db_inscricao_atleta
   add constraint FKm66yp0neo1is0ww5nk3cg9jiu
   foreign key (id_atleta)
   references db_atletas (id);

alter table db_inscricao_atleta
   add constraint FKd77xq61l4s4svgfrkqh2sne9n
   foreign key (id_inscricao_equipe)
   references db_inscricao_equipe (id);

alter table db_inscricao_equipe
   add constraint FKd3ytjo311obm3r86dn7g1t8m0
   foreign key (id_categoria)
   references db_categoria (id);

alter table db_inscricao_equipe
   add constraint FK2j58e535ah467l0ies2ngf07m
   foreign key (id_competicao)
   references db_competicao (id);

alter table db_inscricao_equipe
   add constraint FKndb4p0cssjb8t0kw8aith4uy9
   foreign key (id_equipe)
   references db_equipe (id);
