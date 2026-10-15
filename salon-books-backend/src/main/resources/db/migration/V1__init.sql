-- Salon Books schema. Runs on Supabase Postgres via Flyway.
-- Every table has RLS enabled with NO policies, so the anon/authenticated keys cannot
-- read or write through Supabase's auto REST API. Only the Spring Boot backend
-- (connecting as the postgres role, which bypasses RLS) touches these tables.

create table user_profile (
    id           uuid primary key references auth.users(id) on delete cascade,
    email        text,
    display_name text,
    created_at   timestamptz not null default now()
);

create table business (
    id                   uuid primary key,
    name                 text not null,
    timezone             text not null default 'America/Los_Angeles',
    rent_display         text not null default 'MONTHLY' check (rent_display in ('WEEKLY','MONTHLY')),
    default_expense_sort text not null default 'DATE' check (default_expense_sort in ('DATE','CATEGORY','VENDOR','AMOUNT')),
    created_by           uuid not null references user_profile(id),
    created_at           timestamptz not null default now()
);

create table business_member (
    id          uuid primary key,
    business_id uuid not null references business(id) on delete cascade,
    user_id     uuid not null references user_profile(id) on delete cascade,
    role        text not null check (role in ('OWNER','PARTNER')),
    joined_at   timestamptz not null default now(),
    unique (business_id, user_id)
);
create index business_member_user_idx on business_member(user_id);

create table business_invite (
    id          uuid primary key,
    business_id uuid not null references business(id) on delete cascade,
    token_hash  text not null unique,
    created_by  uuid not null references user_profile(id),
    expires_at  timestamptz not null,
    used_by     uuid references user_profile(id),
    used_at     timestamptz,
    created_at  timestamptz not null default now()
);

-- ---------- Syncable tables (client-generated UUIDs, soft delete, last-write-wins) ----------

create table expense_category (
    id                 uuid primary key,
    business_id        uuid not null references business(id) on delete cascade,
    name               text not null,
    schedule_c_line    text not null,
    color              text,
    default_category   boolean not null default false,
    sort_order         int not null default 0,
    created_at         timestamptz not null default now(),
    updated_at         timestamptz not null default now(),
    client_modified_at timestamptz not null,
    deleted_at         timestamptz
);

create table client (
    id                 uuid primary key,
    business_id        uuid not null references business(id) on delete cascade,
    name               text not null,
    phone              text,
    email              text,
    notes              text,
    created_at         timestamptz not null default now(),
    updated_at         timestamptz not null default now(),
    client_modified_at timestamptz not null,
    deleted_at         timestamptz
);

create table service_type (
    id                       uuid primary key,
    business_id              uuid not null references business(id) on delete cascade,
    name                     text not null,
    default_duration_minutes int check (default_duration_minutes > 0),
    created_at               timestamptz not null default now(),
    updated_at               timestamptz not null default now(),
    client_modified_at       timestamptz not null,
    deleted_at               timestamptz
);

create table appointment (
    id                 uuid primary key,
    business_id        uuid not null references business(id) on delete cascade,
    stylist_id         uuid not null references user_profile(id),
    client_id          uuid references client(id),
    client_label       text,
    service_type_id    uuid references service_type(id),
    starts_at          timestamptz not null,
    ends_at            timestamptz not null,
    status             text not null default 'BOOKED' check (status in ('BOOKED','COMPLETED','CANCELLED','NO_SHOW')),
    notes              text,
    created_at         timestamptz not null default now(),
    updated_at         timestamptz not null default now(),
    client_modified_at timestamptz not null,
    deleted_at         timestamptz,
    check (ends_at > starts_at)
);
create index appointment_day_idx on appointment(business_id, starts_at);

create table income_entry (
    id                 uuid primary key,
    business_id        uuid not null references business(id) on delete cascade,
    stylist_id         uuid not null references user_profile(id),
    appointment_id     uuid references appointment(id),
    client_id          uuid references client(id),
    service_type_id    uuid references service_type(id),
    service_date       date not null,
    service_amount     numeric(12,2) not null check (service_amount >= 0),
    tip_amount         numeric(12,2) not null default 0 check (tip_amount >= 0),
    payment_method     text check (payment_method in ('CASH','CARD','VENMO','ZELLE','CASH_APP','OTHER')),
    notes              text,
    created_at         timestamptz not null default now(),
    updated_at         timestamptz not null default now(),
    client_modified_at timestamptz not null,
    deleted_at         timestamptz
);
create index income_date_idx on income_entry(business_id, service_date);
create index income_client_service_idx on income_entry(business_id, client_id, service_type_id, service_date desc);

create table expense (
    id                 uuid primary key,
    business_id        uuid not null references business(id) on delete cascade,
    expense_date       date not null,
    vendor             text,
    total              numeric(12,2) not null check (total >= 0),
    category_id        uuid references expense_category(id),
    itemized           boolean not null default false,
    receipt_path       text,
    ocr_raw_text       text,
    notes              text,
    created_at         timestamptz not null default now(),
    updated_at         timestamptz not null default now(),
    client_modified_at timestamptz not null,
    deleted_at         timestamptz
);
create index expense_date_idx on expense(business_id, expense_date);

create table expense_item (
    id          uuid primary key,
    expense_id  uuid not null references expense(id) on delete cascade,
    description text not null,
    amount      numeric(12,2) not null,
    category_id uuid references expense_category(id),
    sort_order  int not null default 0
);
create index expense_item_expense_idx on expense_item(expense_id);

create table lease (
    id                 uuid primary key,
    business_id        uuid not null references business(id) on delete cascade,
    label              text,
    amount             numeric(12,2) not null check (amount > 0),
    frequency          text not null check (frequency in ('WEEKLY','MONTHLY')),
    start_date         date not null,
    end_date           date not null,
    notes              text,
    created_at         timestamptz not null default now(),
    updated_at         timestamptz not null default now(),
    client_modified_at timestamptz not null,
    deleted_at         timestamptz,
    check (end_date >= start_date)
);

create index expense_category_sync_idx on expense_category(business_id, updated_at);
create index client_sync_idx           on client(business_id, updated_at);
create index service_type_sync_idx     on service_type(business_id, updated_at);
create index appointment_sync_idx      on appointment(business_id, updated_at);
create index income_sync_idx           on income_entry(business_id, updated_at);
create index expense_sync_idx          on expense(business_id, updated_at);
create index lease_sync_idx            on lease(business_id, updated_at);

-- Lock down the auto-generated REST API.
alter table user_profile     enable row level security;
alter table business         enable row level security;
alter table business_member  enable row level security;
alter table business_invite  enable row level security;
alter table expense_category enable row level security;
alter table client           enable row level security;
alter table service_type     enable row level security;
alter table appointment      enable row level security;
alter table income_entry     enable row level security;
alter table expense          enable row level security;
alter table expense_item     enable row level security;
alter table lease            enable row level security;

-- Private bucket for receipt photos. Uploads/downloads go through backend-issued signed URLs.
insert into storage.buckets (id, name, public)
values ('receipts', 'receipts', false)
on conflict (id) do nothing;
