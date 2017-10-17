create table account (
  id                            bigint auto_increment not null,
  spotify_id                    varchar(255),
  display_name                  varchar(255),
  access_token                  varchar(255),
  refresh_token                 varchar(255),
  selected_device               varchar(255),
  active_party_id               bigint,
  created                       timestamp not null,
  updated                       timestamp not null,
  constraint uq_account_active_party_id unique (active_party_id),
  constraint pk_account primary key (id)
);

create table party (
  id                            bigint auto_increment not null,
  owner_id                      bigint,
  name                          varchar(255),
  description                   varchar(255),
  status                        varchar(7),
  created                       timestamp not null,
  updated                       timestamp not null,
  constraint ck_party_status check ( status in ('ONLINE','OFFLINE')),
  constraint pk_party primary key (id)
);

create table party_queue_entry (
  id                            bigint auto_increment not null,
  party_id                      bigint,
  member_id                     bigint,
  artist                        varchar(255),
  title                         varchar(255),
  thumbnail                     varchar(255),
  uri                           varchar(255),
  votes                         integer not null,
  status                        varchar(9),
  created                       timestamp not null,
  updated                       timestamp not null,
  constraint ck_party_queue_entry_status check ( status in ('PLAYED','IN_QUEUE','CANCELLED')),
  constraint pk_party_queue_entry primary key (id)
);

alter table account add constraint fk_account_active_party_id foreign key (active_party_id) references party (id) on delete restrict on update restrict;

alter table party add constraint fk_party_owner_id foreign key (owner_id) references account (id) on delete restrict on update restrict;
create index ix_party_owner_id on party (owner_id);

alter table party_queue_entry add constraint fk_party_queue_entry_party_id foreign key (party_id) references party (id) on delete restrict on update restrict;
create index ix_party_queue_entry_party_id on party_queue_entry (party_id);

alter table party_queue_entry add constraint fk_party_queue_entry_member_id foreign key (member_id) references account (id) on delete restrict on update restrict;
create index ix_party_queue_entry_member_id on party_queue_entry (member_id);

