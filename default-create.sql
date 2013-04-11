create table mailboxes (
  id                        bigint not null,
  address                   varchar(255),
  ts_active                 bigint,
  expired                   boolean,
  domain                    varchar(255),
  usr_id                    bigint,
  constraint pk_mailboxes primary key (id))
;

create table user (
  id                        bigint not null,
  forename                  varchar(255) not null,
  surname                   varchar(255) not null,
  mail                      varchar(255),
  passwd                    varchar(255) not null,
  admin                     boolean,
  constraint pk_user primary key (id))
;

create sequence mailboxes_seq;

create sequence user_seq;

alter table mailboxes add constraint fk_mailboxes_usr_1 foreign key (usr_id) references user (id) on delete restrict on update restrict;
create index ix_mailboxes_usr_1 on mailboxes (usr_id);


