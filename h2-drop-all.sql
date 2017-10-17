alter table account drop constraint if exists fk_account_active_party_id;

alter table party drop constraint if exists fk_party_owner_id;
drop index if exists ix_party_owner_id;

alter table party_queue_entry drop constraint if exists fk_party_queue_entry_party_id;
drop index if exists ix_party_queue_entry_party_id;

alter table party_queue_entry drop constraint if exists fk_party_queue_entry_member_id;
drop index if exists ix_party_queue_entry_member_id;

drop table if exists account;

drop table if exists party;

drop table if exists party_queue_entry;

