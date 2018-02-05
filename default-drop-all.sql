alter table account drop foreign key fk_account_subscription_id;
drop index ix_account_subscription_id on account;

alter table account drop foreign key fk_account_spotify_id;

alter table account drop foreign key fk_account_login_token_id;
drop index ix_account_login_token_id on account;

alter table account_achievement drop foreign key fk_account_achievement_account;
drop index ix_account_achievement_account on account_achievement;

alter table account_achievement drop foreign key fk_account_achievement_achievement;
drop index ix_account_achievement_achievement on account_achievement;

alter table account_link drop foreign key fk_account_link_account_id;
drop index ix_account_link_account_id on account_link;

alter table favourite_song drop foreign key fk_favourite_song_account_id;
drop index ix_favourite_song_account_id on favourite_song;

alter table follower drop foreign key fk_follower_follower_id;
drop index ix_follower_follower_id on follower;

alter table follower drop foreign key fk_follower_following_id;
drop index ix_follower_following_id on follower;

alter table login_token drop foreign key fk_login_token_account_id;
drop index ix_login_token_account_id on login_token;

alter table party drop foreign key fk_party_owner_id;
drop index ix_party_owner_id on party;

alter table party drop foreign key fk_party_now_playing_id;
drop index ix_party_now_playing_id on party;

alter table party_member drop foreign key fk_party_member_party_id;
drop index ix_party_member_party_id on party_member;

alter table party_member drop foreign key fk_party_member_account_id;
drop index ix_party_member_account_id on party_member;

alter table party_queue_entry drop foreign key fk_party_queue_entry_party_id;
drop index ix_party_queue_entry_party_id on party_queue_entry;

alter table party_queue_entry drop foreign key fk_party_queue_entry_member_id;
drop index ix_party_queue_entry_member_id on party_queue_entry;

alter table party_queue_vote drop foreign key fk_party_queue_vote_account_id;
drop index ix_party_queue_vote_account_id on party_queue_vote;

alter table party_queue_vote drop foreign key fk_party_queue_vote_entry_id;
drop index ix_party_queue_vote_entry_id on party_queue_vote;

alter table playlist_party drop foreign key fk_playlist_party_party_id;

alter table spotify_account drop foreign key fk_spotify_account_active_party_id;
drop index ix_spotify_account_active_party_id on spotify_account;

alter table spotify_account drop foreign key fk_spotify_account_account_id;

drop table if exists account;

drop table if exists account_achievement;

drop table if exists account_link;

drop table if exists achievement;

drop table if exists favourite_song;

drop table if exists follower;

drop table if exists login_token;

drop table if exists party;

drop table if exists party_member;

drop table if exists party_queue_entry;

drop table if exists party_queue_vote;

drop table if exists playlist_party;

drop table if exists spotify_account;

drop table if exists subscription;

drop index ix_account_email on account;
drop index ix_achievement_name on achievement;
drop index ix_favourite_song_uri on favourite_song;
drop index ix_party_member_active on party_member;
drop index ix_playlist_party_playlist_id on playlist_party;
