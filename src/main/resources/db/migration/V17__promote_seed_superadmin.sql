-- Promove o usuário admin criado na V11 para SUPERADMIN (bootstrap).
-- Também garante que ele esteja ativo para o primeiro acesso.
update db_users
set role = 'SUPERADMIN',
    status = 'ATIVO'
where email = 'admin@quixada.gov.br';
