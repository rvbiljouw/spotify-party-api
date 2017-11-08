create table account (
  id                            bigint auto_increment not null,
  account_type                  varchar(7),
  subscription_id               varchar(255),
  email                         varchar(255),
  password                      varchar(255),
  display_picture               varchar(255),
  display_name                  varchar(255),
  spotify_id                    bigint,
  login_token_id                bigint,
  created                       timestamp not null,
  updated                       timestamp not null,
  constraint ck_account_account_type check ( account_type in ('REGULAR','STAFF','BOT')),
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
  created                       timestamp not null,
  updated                       timestamp not null,
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
  created                       timestamp not null,
  updated                       timestamp not null,
  constraint pk_achievement primary key (id)
);

create table login_token (
  id                            bigint auto_increment not null,
  account_id                    bigint,
  status                        varchar(7),
  token                         varchar(255),
  ip_address                    varchar(255),
  user_agent                    varchar(255),
  last_seen                     timestamp,
  created                       timestamp not null,
  updated                       timestamp not null,
  constraint ck_login_token_status check ( status in ('ACTIVE','EXPIRED')),
  constraint pk_login_token primary key (id)
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
  created                       timestamp not null,
  updated                       timestamp not null,
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
  last_seen                     timestamp,
  active                        boolean default false not null,
  created                       timestamp not null,
  updated                       timestamp not null,
  constraint ck_party_member_rank check ( rank in ('VISITOR','MODERATOR','HOST')),
  constraint pk_party_member primary key (id)
);

create table party_queue_entry (
  id                            bigint auto_increment not null,
  party_id                      bigint,
  member_id                     bigint,
  artist                        varchar(255),
  title                         varchar(255),
  thumbnail                     varchar(255),
  duration                      integer not null,
  uri                           varchar(255),
  played_at                     bigint not null,
  votes                         integer not null,
  upvotes                       integer not null,
  downvotes                     integer not null,
  status                        varchar(9),
  created                       timestamp not null,
  updated                       timestamp not null,
  constraint ck_party_queue_entry_status check ( status in ('PLAYED','PLAYING','IN_QUEUE','CANCELLED')),
  constraint pk_party_queue_entry primary key (id)
);

create table party_queue_vote (
  id                            bigint auto_increment not null,
  account_id                    bigint,
  entry_id                      bigint,
  upvote                        boolean,
  created                       timestamp not null,
  updated                       timestamp not null,
  constraint pk_party_queue_vote primary key (id)
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
  created                       timestamp not null,
  updated                       timestamp not null,
  constraint pk_spotify_account primary key (id)
);

create table subscription (
  id                            varchar(255) not null,
  name                          varchar(255),
  description                   varchar(255),
  cost                          decimal(38),
  created                       timestamp not null,
  updated                       timestamp not null,
  constraint pk_subscription primary key (id)
);

create index ix_account_email on account (email);
create index ix_spotify_account_spotify_id on spotify_account (spotify_id);
alter table account add constraint fk_account_subscription_id foreign key (subscription_id) references subscription (id) on delete restrict on update restrict;
create index ix_account_subscription_id on account (subscription_id);

alter table account add constraint fk_account_spotify_id foreign key (spotify_id) references spotify_account (id) on delete restrict on update restrict;
create index ix_account_spotify_id on account (spotify_id);

alter table account add constraint fk_account_login_token_id foreign key (login_token_id) references login_token (id) on delete restrict on update restrict;
create index ix_account_login_token_id on account (login_token_id);

alter table account_achievement add constraint fk_account_achievement_account foreign key (account_id) references account (id) on delete restrict on update restrict;
create index ix_account_achievement_account on account_achievement (account_id);

alter table account_achievement add constraint fk_account_achievement_achievement foreign key (achievement_id) references achievement (id) on delete restrict on update restrict;
create index ix_account_achievement_achievement on account_achievement (achievement_id);

alter table account_link add constraint fk_account_link_account_id foreign key (account_id) references account (id) on delete restrict on update restrict;
create index ix_account_link_account_id on account_link (account_id);

alter table login_token add constraint fk_login_token_account_id foreign key (account_id) references account (id) on delete restrict on update restrict;
create index ix_login_token_account_id on login_token (account_id);

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

alter table spotify_account add constraint fk_spotify_account_active_party_id foreign key (active_party_id) references party (id) on delete restrict on update restrict;
create index ix_spotify_account_active_party_id on spotify_account (active_party_id);

alter table spotify_account add constraint fk_spotify_account_account_id foreign key (account_id) references account (id) on delete restrict on update restrict;
create index ix_spotify_account_account_id on spotify_account (account_id);

