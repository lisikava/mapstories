create table if not exists pins (
    id bigint primary key generated always as identity,
    location point not null,
    category text not null check (category != ''),
    tags hstore not null,
    create_time timestamptz not null default now(),
    update_time timestamptz not null default now()
);

create index if not exists pins_location_gist on pins using gist(location);
create index if not exists pins_tags_gin on pins using gin(tags);

create or replace function set_update_time() returns trigger as $$
begin
    new.update_time := now();
    return new;
end;
$$ language plpgsql;

create or replace trigger set_update_time
    before update on pins
    for each row
    execute function set_update_time();
