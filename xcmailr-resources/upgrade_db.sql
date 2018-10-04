-- some columns were accidently clobs, we try to fix them
alter table mailtransactions alter column relayaddr varchar2(255);
alter table users alter column language varchar2(255);

-- add apitoken to users
alter table users add column if not exists apitoken varchar2(255);

-- create table that will be used to store mails temporarily
create table if not exists mail (
  id                        bigint not null,
  sender                    varchar(255),
  subject                   varchar(255) not null,
  recieve_time              bigint not null,
  message                   clob,
  mailbox_id                bigint,
  constraint pk_mail primary key (id)
);

-- create table that holds mail statistics
create table if not exists MAIL_STATISTICS (
  date                      date not null,
  QUARTER_HOUR              integer not null,
  FROM_DOMAIN               varchar(255) not null,
  TARGET_DOMAIN             varchar(255) not null,
  DROP_COUNT                integer,
  FORWARD_COUNT             integer,
  constraint pk_MAIL_STATISTICS primary key (date, QUARTER_HOUR, FROM_DOMAIN, TARGET_DOMAIN)
);

create sequence if not exists mail_seq;

--
--create sequence if not exists MAIL_STATISTICS_seq;
