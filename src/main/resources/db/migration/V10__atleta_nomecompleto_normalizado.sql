-- =============================================================================
-- V10__atleta_nomecompleto_normalizado.sql
-- Adiciona a coluna `nome_completo_normalizado` à tabela `db_atletas`.
-- =============================================================================

alter table db_atletas add column nome_completo_normalizado varchar(150) not null;

create index idx_atleta_nome_completo_normalizado on db_atletas(nome_completo_normalizado);