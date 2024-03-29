create table account (
  id                            bigint auto_increment not null,
  account_type                  varchar(7),
  subscription_id               varchar(255),
  email                         varchar(255),
  password                      varchar(255),
  display_picture               varchar(255),
  display_name                  varchar(255),
  has_spotify                   tinyint(1) default 0 not null,
  spotify_id                    bigint,
  login_token_id                bigint,
  created                       datetime(6) not null,
  updated                       datetime(6) not null,
  constraint ck_account_account_type check ( account_type in ('REGULAR','STAFF','BOT')),
  constraint uq_account_spotify_id unique (spotify_id),
  constraint pk_account primary key (id)
);

create table account_achievement (
  account_id                    bigint not null,
  achievement_id                bigint not null,
  constraint pk_account_achievement primary key (account_id,achievement_id)
);

create table account_link (
  id                            bigint auto_increment not null,
  external_id                   varchar(255),
  token                         varchar(255),
  account_id                    bigint,
  link_type                     varchar(5),
  created                       datetime(6) not null,
  updated                       datetime(6) not null,
  constraint ck_account_link_link_type check ( link_type in ('SLACK')),
  constraint uq_account_link_external_id unique (external_id),
  constraint uq_account_link_token unique (token),
  constraint pk_account_link primary key (id)
);

create table achievement (
  id                            bigint auto_increment not null,
  name                          varchar(255),
  description                   varchar(255),
  badge_url                     varchar(255),
  created                       datetime(6) not null,
  updated                       datetime(6) not null,
  constraint pk_achievement primary key (id)
);

create table favourite_song (
  id                            bigint auto_increment not null,
  account_id                    bigint,
  type                          varchar(7),
  song_id                       varchar(255),
  artist                        varchar(255),
  title                         varchar(255),
  uri                           varchar(255),
  thumbnail                     varchar(255),
  duration                      integer not null,
  preview_url                   varchar(255),
  uploaded_by                   varchar(255),
  created                       datetime(6) not null,
  updated                       datetime(6) not null,
  constraint ck_favourite_song_type check ( type in ('YOUTUBE','SPOTIFY')),
  constraint pk_favourite_song primary key (id)
);

create table follower (
  id                            bigint auto_increment not null,
  follower_id                   bigint,
  following_id                  bigint,
  created                       datetime(6) not null,
  updated                       datetime(6) not null,
  constraint pk_follower primary key (id)
);

create table login_token (
  id                            bigint auto_increment not null,
  account_id                    bigint,
  status                        varchar(7),
  token                         varchar(255),
  ip_address                    varchar(255),
  user_agent                    varchar(255),
  last_seen                     datetime(6),
  created                       datetime(6) not null,
  updated                       datetime(6) not null,
  constraint ck_login_token_status check ( status in ('ACTIVE','EXPIRED')),
  constraint pk_login_token primary key (id)
);

create table notification (
  id                            bigint auto_increment not null,
  account_id                    bigint,
  interacting_account_id        bigint,
  text                          varchar(255),
  action                        varchar(8),
  `read`                          tinyint(1) default 0 not null,
  created                       datetime(6) not null,
  constraint ck_notification_action check ( action in ('FOLLOWED')),
  constraint pk_notification primary key (id)
);

create table party (
  id                            bigint auto_increment not null,
  owner_id                      bigint,
  active_member_count           integer not null,
  name                          varchar(255),
  description                   varchar(255),
  background_url                varchar(255),
  password                      varchar(255),
  now_playing_id                bigint,
  status                        varchar(7),
  access                        varchar(8),
  type                          varchar(10),
  created                       datetime(6) not null,
  updated                       datetime(6) not null,
  constraint ck_party_status check ( status in ('ONLINE','OFFLINE')),
  constraint ck_party_access check ( access in ('PUBLIC','PRIVATE','PASSWORD')),
  constraint ck_party_type check ( type in ('YOUTUBE','SOUNDCLOUD','SPOTIFY')),
  constraint pk_party primary key (id)
);

create table party_member (
  id                            bigint auto_increment not null,
  rank                          varchar(9),
  party_id                      bigint,
  account_id                    bigint,
  last_seen                     datetime(6),
  active                        tinyint(1) default 0 not null,
  created                       datetime(6) not null,
  updated                       datetime(6) not null,
  constraint ck_party_member_rank check ( rank in ('VISITOR','MODERATOR','HOST')),
  constraint pk_party_member primary key (id)
);

create table party_queue_entry (
  id                            bigint auto_increment not null,
  party_id                      bigint,
  member_id                     bigint,
  song_id                       varchar(255),
  artist                        varchar(255),
  title                         varchar(255),
  thumbnail                     varchar(255),
  duration                      integer not null,
  uri                           varchar(255),
  uploaded_by                   varchar(255),
  played_at                     bigint not null,
  votes                         integer not null,
  upvotes                       integer not null,
  downvotes                     integer not null,
  votes_to_skip                 integer not null,
  status                        varchar(9),
  created                       datetime(6) not null,
  updated                       datetime(6) not null,
  constraint ck_party_queue_entry_status check ( status in ('PLAYED','PLAYING','IN_QUEUE','CANCELLED','SKIPPED')),
  constraint pk_party_queue_entry primary key (id)
);

create table party_queue_vote (
  id                            bigint auto_increment not null,
  account_id                    bigint,
  entry_id                      bigint,
  upvote                        tinyint(1) default 0,
  vote_to_skip                  tinyint(1) default 0,
  created                       datetime(6) not null,
  updated                       datetime(6) not null,
  constraint pk_party_queue_vote primary key (id)
);

create table playlist_party (
  id                            bigint auto_increment not null,
  party_id                      bigint,
  playlist_id                   varchar(255),
  playlist_owner_id             varchar(255),
  created                       datetime(6) not null,
  updated                       datetime(6) not null,
  constraint uq_playlist_party_party_id unique (party_id),
  constraint pk_playlist_party primary key (id)
);

create table spotify_account (
  id                            bigint auto_increment not null,
  spotify_id                    varchar(255),
  active_party_id               bigint,
  account_id                    bigint,
  display_name                  varchar(255),
  access_token                  varchar(255),
  refresh_token                 varchar(255),
  device                        varchar(255),
  created                       datetime(6) not null,
  updated                       datetime(6) not null,
  constraint uq_spotify_account_spotify_id unique (spotify_id),
  constraint uq_spotify_account_account_id unique (account_id),
  constraint pk_spotify_account primary key (id)
);

create table subscription (
  id                            varchar(255) not null,
  name                          varchar(255),
  description                   varchar(255),
  cost                          decimal(38),
  created                       datetime(6) not null,
  updated                       datetime(6) not null,
  constraint pk_subscription primary key (id)
);

create index ix_account_email on account (email);
create index ix_achievement_name on achievement (name);
create index ix_favourite_song_uri on favourite_song (uri);
create index ix_notification_action on notification (action);
create index ix_notification_read on notification (read);
create index ix_party_member_active on party_member (active);
create index ix_playlist_party_playlist_id on playlist_party (playlist_id);
alter table account add constraint fk_account_subscription_id foreign key (subscription_id) references subscription (id) on delete restrict on update restrict;
create index ix_account_subscription_id on account (subscription_id);

alter table account add constraint fk_account_spotify_id foreign key (spotify_id) references spotify_account (id) on delete restrict on update restrict;

alter table account add constraint fk_account_login_token_id foreign key (login_token_id) references login_token (id) on delete restrict on update restrict;
create index ix_account_login_token_id on account (login_token_id);

alter table account_achievement add constraint fk_account_achievement_account foreign key (account_id) references account (id) on delete restrict on update restrict;
create index ix_account_achievement_account on account_achievement (account_id);

alter table account_achievement add constraint fk_account_achievement_achievement foreign key (achievement_id) references achievement (id) on delete restrict on update restrict;
create index ix_account_achievement_achievement on account_achievement (achievement_id);

alter table account_link add constraint fk_account_link_account_id foreign key (account_id) references account (id) on delete restrict on update restrict;
create index ix_account_link_account_id on account_link (account_id);

alter table favourite_song add constraint fk_favourite_song_account_id foreign key (account_id) references account (id) on delete restrict on update restrict;
create index ix_favourite_song_account_id on favourite_song (account_id);

alter table follower add constraint fk_follower_follower_id foreign key (follower_id) references account (id) on delete restrict on update restrict;
create index ix_follower_follower_id on follower (follower_id);

alter table follower add constraint fk_follower_following_id foreign key (following_id) references account (id) on delete restrict on update restrict;
create index ix_follower_following_id on follower (following_id);

alter table login_token add constraint fk_login_token_account_id foreign key (account_id) references account (id) on delete restrict on update restrict;
create index ix_login_token_account_id on login_token (account_id);

alter table notification add constraint fk_notification_account_id foreign key (account_id) references account (id) on delete restrict on update restrict;
create index ix_notification_account_id on notification (account_id);

alter table notification add constraint fk_notification_interacting_account_id foreign key (interacting_account_id) references account (id) on delete restrict on update restrict;
create index ix_notification_interacting_account_id on notification (interacting_account_id);

alter table party add constraint fk_party_owner_id foreign key (owner_id) references account (id) on delete restrict on update restrict;
create index ix_party_owner_id on party (owner_id);

alter table party add constraint fk_party_now_playing_id foreign key (now_playing_id) references party_queue_entry (id) on delete restrict on update restrict;
create index ix_party_now_playing_id on party (now_playing_id);

alter table party_member add constraint fk_party_member_party_id foreign key (party_id) references party (id) on delete restrict on update restrict;
create index ix_party_member_party_id on party_member (party_id);

alter table party_member add constraint fk_party_member_account_id foreign key (account_id) references account (id) on delete restrict on update restrict;
create index ix_party_member_account_id on party_member (account_id);

alter table party_queue_entry add constraint fk_party_queue_entry_party_id foreign key (party_id) references party (id) on delete restrict on update restrict;
create index ix_party_queue_entry_party_id on party_queue_entry (party_id);

alter table party_queue_entry add constraint fk_party_queue_entry_member_id foreign key (member_id) references account (id) on delete restrict on update restrict;
create index ix_party_queue_entry_member_id on party_queue_entry (member_id);

alter table party_queue_vote add constraint fk_party_queue_vote_account_id foreign key (account_id) references account (id) on delete restrict on update restrict;
create index ix_party_queue_vote_account_id on party_queue_vote (account_id);

alter table party_queue_vote add constraint fk_party_queue_vote_entry_id foreign key (entry_id) references party_queue_entry (id) on delete restrict on update restrict;
create index ix_party_queue_vote_entry_id on party_queue_vote (entry_id);

alter table playlist_party add constraint fk_playlist_party_party_id foreign key (party_id) references party (id) on delete restrict on update restrict;

alter table spotify_account add constraint fk_spotify_account_active_party_id foreign key (active_party_id) references party (id) on delete restrict on update restrict;
create index ix_spotify_account_active_party_id on spotify_account (active_party_id);

alter table spotify_account add constraint fk_spotify_account_account_id foreign key (account_id) references account (id) on delete restrict on update restrict;

