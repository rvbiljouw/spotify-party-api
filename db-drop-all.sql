alter table account drop foreign key fk_account_active_party_id;

alter table party drop foreign key fk_party_owner_id;
drop index ix_party_owner_id on party;

alter table party_account drop foreign key fk_party_account_party;
drop index ix_party_account_party on party_account;

alter table party_account drop foreign key fk_party_account_account;
drop index ix_party_account_account on party_account;

alter table active_party_members drop foreign key fk_active_party_members_party;
drop index ix_active_party_members_party on active_party_members;

alter table active_party_members drop foreign key fk_active_party_members_account;
drop index ix_active_party_members_account on active_party_members;

alter table party_queue_entry drop foreign key fk_party_queue_entry_party_id;
drop index ix_party_queue_entry_party_id on party_queue_entry;

alter table party_queue_entry drop foreign key fk_party_queue_entry_member_id;
drop index ix_party_queue_entry_member_id on party_queue_entry;

alter table party_queue_vote drop foreign key fk_party_queue_vote_account_id;
drop index ix_party_queue_vote_account_id on party_queue_vote;

alter table party_queue_vote drop foreign key fk_party_queue_vote_entry_id;
drop index ix_party_queue_vote_entry_id on party_queue_vote;

drop table if exists account;

drop table if exists party;

drop table if exists party_account;

drop table if exists active_party_members;

drop table if exists party_queue_entry;

drop table if exists party_queue_vote;

drop index ix_account_access_token on account;
drop index ix_account_selected_device on account;
