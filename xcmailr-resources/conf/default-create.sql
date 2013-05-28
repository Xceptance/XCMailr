create table if not exists mailboxes (
  id                        bigint not null,
  address                   varchar(255),
  ts_active                 bigint,
  expired                   boolean,
  domain                    varchar(255),
  forwards                  integer,
  suppressions              integer,
  usr_id                    bigint,
  version                   bigint not null,
  constraint pk_mailboxes primary key (id))
;

create table if not exists mailtransactions (
  id                        bigint not null,
  ts                        bigint,
  status                    integer,
  sourceaddr                varchar(255),
  targetaddr                varchar(255),
  constraint pk_mailtransactions primary key (id))
;

create table if not exists users (
  id                        bigint not null,
  forename                  varchar(255),
  surname                   varchar(255),
  mail                      varchar(255),
  passwd                    varchar(255),
  admin                     boolean,
  active                    boolean,
  confirmation              varchar(255),
  ts_confirm                bigint,
  bad_pw_count              integer,
  constraint pk_users primary key (id))
;

create sequence if not exists mailboxes_seq;

create sequence if not exists mailtransactions_seq;

create sequence if not exists users_seq;

alter table mailboxes add constraint fk_mailboxes_usr_1 foreign key (usr_id) references users (id) on delete restrict on update restrict;
create index if not exists ix_mailboxes_usr_1 on mailboxes (usr_id);


