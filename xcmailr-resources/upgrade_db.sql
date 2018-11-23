--rename column to fix typo
alter table mail alter column recieve_time rename to receive_time;

-- add flag indicating whether received emails should be forwaded or not
alter table MAILBOXES add column if not exists forward_emails boolean default true;

-- add field to save timestamp of api token. used to delete it automatically later
alter table users add column if not exists API_TOKEN_CREATION_TIMESTAMP bigint default 0;

