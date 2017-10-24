alter table account drop constraint if exists fk_account_active_party_id;
drop index if exists ix_account_active_party_id;

alter table account_link drop constraint if exists fk_account_link_account_id;
drop index if exists ix_account_link_account_id;

alter table party drop constraint if exists fk_party_owner_id;
drop index if exists ix_party_owner_id;

alter table party_account drop constraint if exists fk_party_account_party;
drop index if exists ix_party_account_party;

alter table party_account drop constraint if exists fk_party_account_account;
drop index if exists ix_party_account_account;

alter table active_party_members drop constraint if exists fk_active_party_members_party;
drop index if exists ix_active_party_members_party;

alter table active_party_members drop constraint if exists fk_active_party_members_account;
drop index if exists ix_active_party_members_account;

alter table party_queue_entry drop constraint if exists fk_party_queue_entry_party_id;
drop index if exists ix_party_queue_entry_party_id;

alter table party_queue_entry drop constraint if exists fk_party_queue_entry_member_id;
drop index if exists ix_party_queue_entry_member_id;

alter table party_queue_vote drop constraint if exists fk_party_queue_vote_account_id;
drop index if exists ix_party_queue_vote_account_id;

alter table party_queue_vote drop constraint if exists fk_party_queue_vote_entry_id;
drop index if exists ix_party_queue_vote_entry_id;

drop table if exists account;

drop table if exists account_link;

drop table if exists party;

drop table if exists party_account;

drop table if exists active_party_members;

drop table if exists party_queue_entry;

drop table if exists party_queue_vote;

drop index if exists ix_account_login_token;
drop index if exists ix_account_access_token;
drop index if exists ix_account_selected_device;
