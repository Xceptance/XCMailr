create table if not exists register_domains (
  id                        bigint not null,
  domainname                varchar(255),
  constraint pk_register_domains primary key (id))
;

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
  relayaddr                 varchar(255),
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
  language                  varchar(255),
  constraint pk_users primary key (id))
;

create TABLE if not exists MAIL_STATISTICS(
  ID                        bigint not null,
  DATE                      date not null,
  QUARTER_HOUR              int not null,
  FROM_DOMAIN               varchar(50) not null,
  TARGET_DOMAIN             varchar(50) not null,
  DROP_COUNT                int,
  FORWARD_COUNT             int,
  constraint pk_mail_statistics primary key (ID)
);

CREATE UNIQUE INDEX "MAIL_STATISTICS_INDEX" on MAIL_STATISTICS(DATE, QUARTER_HOUR, FROM_DOMAIN, TARGET_DOMAIN);

create sequence if not exists register_domains_seq;

create sequence if not exists mailboxes_seq;

create sequence if not exists mailtransactions_seq;

create sequence if not exists users_seq;

create sequence if not exists mail_statistics_seq;

create index if not exists ix_mailboxes_usr_1 on mailboxes (usr_id);
create index if not exists ix_mailtransactions_ts_1 on mailtransactions (ts);


create or replace view V_EMAIL_STATISTICS_24("DATE", "QUARTER_HOUR", "DROPPED_MAIL_COUNT", "FORWARDED_MAIL_COUNT") as
(
	select ms.DATE as "DATE", ms.QUARTER_HOUR as "QUARTER_HOUR", SUM(ms.DROP_COUNT) as "DROPPED_MAIL_COUNT", SUM(ms.FORWARD_COUNT) as "FORWARDED_MAIL_COUNT"
  from MAIL_STATISTICS ms
 where  -- current day
       (ms.DATE = CURRENT_DATE()
        and ms.QUARTER_HOUR <= (select hour(CURRENT_TIME()) * 4 + (MINUTE(CURRENT_TIME()) / 15)))
    or -- yesterday, but only from the current quarter hour +1 until end of the day
       (ms.DATE = CURRENT_DATE() - 1
        and ms.QUARTER_HOUR > (select hour(CURRENT_TIME()) * 4 + (MINUTE(CURRENT_TIME()) / 15))
    )
  group by ms.DATE, ms.QUARTER_HOUR
);