-- =============================================================================
-- V3__categoria_status.sql
-- Adiciona o `status` enum (ATIVO/INATIVO) à categoria, adotando o mesmo
-- padrão de soft delete do Atleta. Linhas existentes ficam ATIVO.
-- =============================================================================

alter table db_categoria
    add column status ENUM('ATIVO','INATIVO') not null default 'ATIVO';
