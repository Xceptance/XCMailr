SET REFERENTIAL_INTEGRITY FALSE;

drop table if exists mailboxes;

drop table if exists user;

SET REFERENTIAL_INTEGRITY TRUE;

drop sequence if exists mailboxes_seq;

drop sequence if exists user_seq;

