-- =============================================================================
-- V5__modalidade_status.sql
-- Substitui o booleano `ativo` pelo `status` enum (ATIVO/INATIVO), adotando o
-- mesmo padrão de soft delete do Atleta. Linhas existentes mantêm seu estado.
-- =============================================================================

alter table db_modalidade add column status ENUM('ATIVO','INATIVO') not null default 'ATIVO';

update db_modalidade set status = case when ativo then 'ATIVO' else 'INATIVO' end;

alter table db_modalidade drop column ativo;
