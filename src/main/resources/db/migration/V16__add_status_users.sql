alter table db_users
    add column status enum ('ATIVO','INATIVO') not null default 'ATIVO';
