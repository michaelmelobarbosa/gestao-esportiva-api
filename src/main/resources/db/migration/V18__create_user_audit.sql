create table db_user_audit (
    id bigint not null auto_increment,
    occurred_at datetime(6) not null,
    actor_id bigint not null,
    actor_email varchar(254) not null,
    action varchar(20) not null,
    target_id bigint not null,
    target_email varchar(254) not null,
    changes text,
    primary key (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create index idx_user_audit_occurred_at on db_user_audit (occurred_at);
