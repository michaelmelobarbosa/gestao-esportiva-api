-- =============================================================================
-- V4__equipe_status.sql
-- Substitui o booleano `ativo` pelo `status` enum (ATIVO/INATIVO), adotando o
-- mesmo padrão de soft delete do Atleta. Linhas existentes ficam ATIVO.
-- =============================================================================

alter table db_equipe add column status ENUM('ATIVO','INATIVO') not null default 'ATIVO';

alter table db_equipe drop column ativo;
