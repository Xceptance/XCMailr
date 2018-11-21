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
alter table mail alter column recieve_time rename to receive_time;

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



alter table MAILBOXES add column if not exists forward_emails boolean default true;
alter table users add column if not exists API_TOKEN_EXPIRATION bigint default 0;