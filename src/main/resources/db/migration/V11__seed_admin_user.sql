-- =============================================================================
-- V11__seed_admin_user.sql
-- Garante a existência de um usuário ADMIN padrão.
-- Idempotente: só insere se ainda não existir um usuário com username 'admin'.
--
-- Credenciais padrão:  admin / admin@123   (TROQUE EM PRODUÇÃO)
-- O hash abaixo é BCrypt (custo 10) gerado pelo BCryptPasswordEncoder do projeto.
-- =============================================================================

insert into db_users (username, email, password, role)
select 'admin', 'admin@quixada.gov.br', '$2a$10$66za6yJLpskeyK0AcSdVvOUunEiBrRRe1UmfBm/w5DwDeCVlFPEbC', 'ADMIN'
from dual
where not exists (
    select 1 from db_users where username = 'admin'
);
