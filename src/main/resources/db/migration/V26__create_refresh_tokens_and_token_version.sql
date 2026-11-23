alter table db_users
    add column token_version integer not null default 0;

create table db_refresh_tokens (
    id bigint not null auto_increment,
    user_id bigint not null,
    token_hash varchar(64) not null,
    expires_at datetime(6) not null,
    revoked_at datetime(6) null,
    created_at datetime(6) not null,
    primary key (id),
    constraint uk_refresh_tokens_hash unique (token_hash),
    constraint fk_refresh_tokens_user foreign key (user_id) references db_users (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create index idx_refresh_tokens_user_id on db_refresh_tokens (user_id);
