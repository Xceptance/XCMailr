create table mailboxes (
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

create table users (
  id                        bigint not null,
  forename                  varchar(255) not null,
  surname                   varchar(255) not null,
  mail                      varchar(255),
  passwd                    varchar(255) not null,
  admin                     boolean,
  active                    boolean,
  constraint pk_users primary key (id))
;

create sequence mailboxes_seq;

create sequence users_seq;

alter table mailboxes add constraint fk_mailboxes_usr_1 foreign key (usr_id) references users (id) on delete restrict on update restrict;
create index ix_mailboxes_usr_1 on mailboxes (usr_id);


