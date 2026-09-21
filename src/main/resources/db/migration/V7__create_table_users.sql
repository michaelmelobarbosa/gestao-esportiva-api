create table db_users (
                        id bigint not null auto_increment,
                        username varchar(20) not null,
                        email varchar(20) not null,
                        password varchar(255) not null,
                        roles enum('ADMIN', 'USER') not null,
                        primary key (id)

) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;