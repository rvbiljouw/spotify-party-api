alter table account drop constraint if exists fk_account_subscription_id;
drop index if exists ix_account_subscription_id;

alter table account drop constraint if exists fk_account_spotify_id;
drop index if exists ix_account_spotify_id;

alter table account drop constraint if exists fk_account_login_token_id;
drop index if exists ix_account_login_token_id;

alter table account_achievement drop constraint if exists fk_account_achievement_account;
drop index if exists ix_account_achievement_account;

alter table account_achievement drop constraint if exists fk_account_achievement_achievement;
drop index if exists ix_account_achievement_achievement;

alter table account_link drop constraint if exists fk_account_link_account_id;
drop index if exists ix_account_link_account_id;

alter table login_token drop constraint if exists fk_login_token_account_id;
drop index if exists ix_login_token_account_id;

alter table party drop constraint if exists fk_party_owner_id;
drop index if exists ix_party_owner_id;

alter table party drop constraint if exists fk_party_now_playing_id;
drop index if exists ix_party_now_playing_id;

alter table party_member drop constraint if exists fk_party_member_party_id;
drop index if exists ix_party_member_party_id;

alter table party_member drop constraint if exists fk_party_member_account_id;
drop index if exists ix_party_member_account_id;

alter table party_queue_entry drop constraint if exists fk_party_queue_entry_party_id;
drop index if exists ix_party_queue_entry_party_id;

alter table party_queue_entry drop constraint if exists fk_party_queue_entry_member_id;
drop index if exists ix_party_queue_entry_member_id;

alter table party_queue_vote drop constraint if exists fk_party_queue_vote_account_id;
drop index if exists ix_party_queue_vote_account_id;

alter table party_queue_vote drop constraint if exists fk_party_queue_vote_entry_id;
drop index if exists ix_party_queue_vote_entry_id;

alter table spotify_account drop constraint if exists fk_spotify_account_active_party_id;
drop index if exists ix_spotify_account_active_party_id;

alter table spotify_account drop constraint if exists fk_spotify_account_account_id;
drop index if exists ix_spotify_account_account_id;

drop table if exists account;

drop table if exists account_achievement;

drop table if exists account_link;

drop table if exists achievement;

drop table if exists login_token;

drop table if exists party;

drop table if exists party_member;

drop table if exists party_queue_entry;

drop table if exists party_queue_vote;

drop table if exists spotify_account;

drop table if exists subscription;

drop index if exists ix_account_email;
drop index if exists ix_party_member_active;
drop index if exists ix_spotify_account_spotify_id;
