-- Tables for KrishiYan. Run first.

create extension if not exists btree_gist;

create table public.profiles (
  id uuid primary key references auth.users (id) on delete cascade,
  name text not null check (length(trim(name)) > 0),
  phone text not null check (length(trim(phone)) >= 10),
  village text not null check (length(trim(village)) > 0),
  created_at timestamptz not null default now()
);

create table public.equipment (
  id uuid primary key default gen_random_uuid(),
  owner_id uuid not null references public.profiles (id) on delete cascade,
  name text not null check (length(trim(name)) > 0),
  category text not null check (category in
    ('tractor', 'harvester', 'rotavator', 'cultivator', 'seed_drill', 'sprayer', 'thresher', 'other')),
  brand_model text not null default '',
  description text not null default '',
  year int check (year between 1950 and 2100),
  price numeric(10, 2) not null check (price > 0),
  price_unit text not null check (price_unit in ('hour', 'day')),
  lat double precision not null check (lat between -90 and 90),
  lng double precision not null check (lng between -180 and 180),
  is_available boolean not null default true,
  created_at timestamptz not null default now()
);

create index equipment_owner_idx on public.equipment (owner_id);

create table public.equipment_images (
  id uuid primary key default gen_random_uuid(),
  equipment_id uuid not null references public.equipment (id) on delete cascade,
  storage_path text not null,
  position int not null default 0
);

create index equipment_images_equipment_idx on public.equipment_images (equipment_id, position);

create table public.equipment_blocked_dates (
  id uuid primary key default gen_random_uuid(),
  equipment_id uuid not null references public.equipment (id) on delete cascade,
  start_at timestamptz not null,
  end_at timestamptz not null,
  check (end_at > start_at)
);

create index equipment_blocked_dates_equipment_idx on public.equipment_blocked_dates (equipment_id);

create table public.bookings (
  id uuid primary key default gen_random_uuid(),
  equipment_id uuid not null references public.equipment (id) on delete cascade,
  renter_id uuid not null references public.profiles (id) on delete cascade,
  owner_id uuid not null references public.profiles (id) on delete cascade,
  start_at timestamptz not null,
  end_at timestamptz not null,
  estimated_cost numeric(10, 2) not null,
  message text not null default '',
  status text not null default 'pending'
    check (status in ('pending', 'accepted', 'rejected', 'cancelled', 'completed')),
  created_at timestamptz not null default now(),
  check (end_at > start_at),
  check (renter_id <> owner_id),
  -- Two accepted bookings for the same equipment can never overlap in time.
  constraint bookings_no_overlap exclude using gist (
    equipment_id with =,
    tstzrange(start_at, end_at) with &&
  ) where (status = 'accepted')
);

create index bookings_renter_idx on public.bookings (renter_id);
create index bookings_owner_idx on public.bookings (owner_id);
