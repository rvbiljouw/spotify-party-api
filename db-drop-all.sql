alter table account drop foreign key fk_account_active_party_id;

alter table account_party drop foreign key fk_account_party_account;
drop index ix_account_party_account on account_party;

alter table account_party drop foreign key fk_account_party_party;
drop index ix_account_party_party on account_party;

alter table party drop foreign key fk_party_owner_id;
drop index ix_party_owner_id on party;

alter table party_account drop foreign key fk_party_account_party;
drop index ix_party_account_party on party_account;

alter table party_account drop foreign key fk_party_account_account;
drop index ix_party_account_account on party_account;

alter table party_queue_entry drop foreign key fk_party_queue_entry_party_id;
drop index ix_party_queue_entry_party_id on party_queue_entry;

alter table party_queue_entry drop foreign key fk_party_queue_entry_member_id;
drop index ix_party_queue_entry_member_id on party_queue_entry;

drop table if exists account;

drop table if exists account_party;

drop table if exists party;

drop table if exists party_account;

drop table if exists party_queue_entry;

