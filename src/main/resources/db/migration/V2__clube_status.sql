-- =============================================================================
-- V2__clube_status.sql
-- Substitui o booleano `ativo` pelo `status` enum (ATIVO/INATIVO), adotando o
-- mesmo padrão de soft delete do Atleta.
-- =============================================================================

alter table db_clube add column status ENUM('ATIVO','INATIVO') not null default 'ATIVO';

update db_clube set status = case when ativo then 'ATIVO' else 'INATIVO' end;

alter table db_clube drop column ativo;
