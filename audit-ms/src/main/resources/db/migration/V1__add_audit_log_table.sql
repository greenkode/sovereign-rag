create table if not exists audit_log
(
    id            uuid         not null primary key,
    identity      varchar(255) not null,
    identity_type varchar(255) not null,
    resource      varchar(100) not null,
    event         varchar(100) not null,
    event_time    timestamp    not null,
    time_recorded timestamp    not null,
    payload       text         not null
);
