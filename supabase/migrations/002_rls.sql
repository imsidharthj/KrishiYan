-- Access rules. Run second.
-- Equipment rows hold the exact location, so only the owner reads them directly.
-- Everyone else reads equipment through the functions in 003, which return an approximate location.
-- Bookings change status only through the functions in 003.

alter table public.profiles enable row level security;
alter table public.equipment enable row level security;
alter table public.equipment_images enable row level security;
alter table public.equipment_blocked_dates enable row level security;
alter table public.bookings enable row level security;

revoke all on public.profiles, public.equipment, public.equipment_images,
  public.equipment_blocked_dates, public.bookings from anon, authenticated;

grant select, insert, update on public.profiles to authenticated;
grant select, insert, update, delete on public.equipment to authenticated;
grant select, insert, update, delete on public.equipment_images to authenticated;
grant select, insert, update, delete on public.equipment_blocked_dates to authenticated;
grant select on public.bookings to authenticated;

create policy "read own profile" on public.profiles
  for select to authenticated using (id = auth.uid());
create policy "create own profile" on public.profiles
  for insert to authenticated with check (id = auth.uid());
create policy "update own profile" on public.profiles
  for update to authenticated using (id = auth.uid()) with check (id = auth.uid());

create policy "owner manages equipment" on public.equipment
  for all to authenticated using (owner_id = auth.uid()) with check (owner_id = auth.uid());

create or replace function public.owns_equipment(p_equipment_id uuid)
returns boolean language sql stable security definer set search_path = public as $$
  select exists (select 1 from equipment where id = p_equipment_id and owner_id = auth.uid());
$$;

create or replace function public.is_listed_equipment(p_equipment_id uuid)
returns boolean language sql stable security definer set search_path = public as $$
  select exists (select 1 from equipment where id = p_equipment_id and is_available);
$$;

create policy "read images of listed equipment" on public.equipment_images
  for select to authenticated
  using (public.is_listed_equipment(equipment_id) or public.owns_equipment(equipment_id));
create policy "owner manages images" on public.equipment_images
  for all to authenticated
  using (public.owns_equipment(equipment_id)) with check (public.owns_equipment(equipment_id));

create policy "read blocked dates of listed equipment" on public.equipment_blocked_dates
  for select to authenticated
  using (public.is_listed_equipment(equipment_id) or public.owns_equipment(equipment_id));
create policy "owner manages blocked dates" on public.equipment_blocked_dates
  for all to authenticated
  using (public.owns_equipment(equipment_id)) with check (public.owns_equipment(equipment_id));

create policy "renter and owner read booking" on public.bookings
  for select to authenticated using (renter_id = auth.uid() or owner_id = auth.uid());
