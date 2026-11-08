create table db_partida (
    id bigint not null auto_increment,
    rodada integer not null,
    data_hora datetime(6) not null,
    local varchar(150),
    status enum ('AGENDADA','EM_ANDAMENTO','FINALIZADA','CANCELADA') not null,
    gols_mandante integer,
    gols_visitante integer,
    id_inscricao_mandante bigint not null,
    id_inscricao_visitante bigint not null,
    primary key (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

alter table db_partida
    add constraint fk_partida_inscricao_mandante
    foreign key (id_inscricao_mandante)
    references db_inscricao_equipe (id);

alter table db_partida
    add constraint fk_partida_inscricao_visitante
    foreign key (id_inscricao_visitante)
    references db_inscricao_equipe (id);
