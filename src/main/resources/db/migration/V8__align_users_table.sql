alter table db_users
    change column roles role varchar(20) not null;

alter table db_users
    modify column email varchar(20) null;
