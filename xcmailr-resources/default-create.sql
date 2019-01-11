create table register_domains (
  id                        bigint not null,
  domainname                varchar(255),
  constraint pk_register_domains primary key (id))
;

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
  forward_emails            boolean default true,
  constraint pk_mailboxes primary key (id))
;

create table mail (
  id                        bigint not null,
  sender                    varchar(255),
  subject                   varchar(255) not null,
  receive_time              bigint not null,
  message                   clob,
  mailbox_id                bigint,
  constraint pk_mail primary key (id)),
  uuid                      varchar(36)
;

create table MAIL_STATISTICS (
  date                      date not null,
  QUARTER_HOUR              integer not null,
  FROM_DOMAIN               varchar(255) not null,
  TARGET_DOMAIN             varchar(255) not null,
  DROP_COUNT                integer,
  FORWARD_COUNT             integer,
  constraint pk_MAIL_STATISTICS primary key (date, QUARTER_HOUR, FROM_DOMAIN, TARGET_DOMAIN))
;

create table mailtransactions (
  id                        bigint not null,
  ts                        bigint,
  status                    integer,
  sourceaddr                varchar(255),
  relayaddr                 varchar(255),
  targetaddr                varchar(255),
  constraint pk_mailtransactions primary key (id))
;

create table users (
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
  language                  varchar(255),
  apitoken                  varchar(255),
  API_TOKEN_CREATION_TIMESTAMP bigint default 0,
  constraint pk_users primary key (id))
;

create sequence register_domains_seq;

create sequence mailboxes_seq;

create sequence mail_seq;

create sequence MAIL_STATISTICS_seq;

create sequence mailtransactions_seq;

create sequence users_seq;

alter table mailboxes add constraint fk_mailboxes_usr_1 foreign key (usr_id) references users (id) on delete restrict on update restrict;
create index ix_mailboxes_usr_1 on mailboxes (usr_id);
alter table mail add constraint fk_mail_mailbox_2 foreign key (mailbox_id) references mailboxes (id) on delete restrict on update restrict;
create index ix_mail_mailbox_2 on mail (mailbox_id);


