create table if not exists pins (
    id bigint primary key generated always as identity,
    location point not null,
    category text not null check (category != ''),
    tags hstore not null,
    create_time timestamptz not null default now(),
    update_time timestamptz not null default now()
);

create table if not exists subscriptions (
    id bigint primary key generated always as identity,
    email text not null check (email != ''),
    tz_offset int not null check (tz_offset between 0 and 1439),
    pattern jsonb not null check (pattern is json object)
);

create table lost_and_found_matches (
    lost bigint references pins,
    found bigint references pins,
    primary key (lost, found)
);

create index if not exists pins_location_gist on pins using gist(location);
create index if not exists pins_category_idx on pins(category);
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

create or replace function is_subcategory_of(sub text, super text)
returns boolean
language sql
immutable
returns null on null input
as $$
    select sub = super or sub like super || '.%';
$$;
