create table if not exists pins (
    id bigint primary key generated always as identity,
    location point not null,
    category text not null check (category != ''),
    tags jsonb not null check (tags is json object)
);
