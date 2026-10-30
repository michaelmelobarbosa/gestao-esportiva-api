alter table db_users
    add unique index uk_db_users_email (email);
