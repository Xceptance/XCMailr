SET REFERENTIAL_INTEGRITY FALSE;

drop table if exists mailboxes;

drop table if exists mailtransactions;

drop table if exists users;

SET REFERENTIAL_INTEGRITY TRUE;

drop sequence if exists mailboxes_seq;

drop sequence if exists mailtransactions_seq;

drop sequence if exists users_seq;

