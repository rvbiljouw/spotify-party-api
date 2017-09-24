create table account (
  id                            bigint auto_increment not null,
  spotify_id                    varchar(255),
  display_name                  varchar(255),
  access_token                  varchar(255),
  refresh_token                 varchar(255),
  created                       timestamp not null,
  updated                       timestamp not null,
  constraint pk_account primary key (id)
);

