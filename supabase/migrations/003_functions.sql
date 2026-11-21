-- Functions the app calls. Run third.
-- They run as the database owner (security definer) and check auth.uid() themselves.

-- About 1 km of rounding so the owner's home is never shown exactly.
create or replace function public.approximate(p_coord double precision)
returns double precision language sql immutable as $$
  select round(p_coord::numeric, 2)::double precision;
$$;

create or replace function public.haversine_km(lat1 double precision, lng1 double precision,
                                               lat2 double precision, lng2 double precision)
returns double precision language sql immutable as $$
  select 2 * 6371 * asin(sqrt(
    power(sin(radians(lat2 - lat1) / 2), 2) +
    cos(radians(lat1)) * cos(radians(lat2)) * power(sin(radians(lng2 - lng1) / 2), 2)
  ));
$$;

create or replace function public.is_busy_now(p_equipment_id uuid)
returns boolean language sql stable security definer set search_path = public as $$
  select exists (select 1 from equipment_blocked_dates
                 where equipment_id = p_equipment_id and now() >= start_at and now() < end_at)
      or exists (select 1 from bookings
                 where equipment_id = p_equipment_id and status = 'accepted'
                   and now() >= start_at and now() < end_at);
$$;

create or replace function public.nearby_equipment(
  p_lat double precision,
  p_lng double precision,
  p_radius_km double precision,
  p_category text default null,
  p_query text default null
)
returns table (
  id uuid, name text, category text, price numeric, price_unit text,
  approx_lat double precision, approx_lng double precision, distance_km int,
  available_now boolean, photo_path text, created_at timestamptz
)
language sql stable security definer set search_path = public as $$
  select e.id, e.name, e.category, e.price, e.price_unit,
         approximate(e.lat), approximate(e.lng),
         greatest(1, round(haversine_km(p_lat, p_lng, e.lat, e.lng)))::int,
         not is_busy_now(e.id),
         (select storage_path from equipment_images i where i.equipment_id = e.id order by position limit 1),
         e.created_at
  from equipment e
  where auth.uid() is not null
    and e.is_available
    and haversine_km(p_lat, p_lng, e.lat, e.lng) <= p_radius_km
    and (p_category is null or e.category = p_category)
    and (p_query is null or e.name ilike '%' || p_query || '%' or e.brand_model ilike '%' || p_query || '%')
  order by haversine_km(p_lat, p_lng, e.lat, e.lng), e.created_at desc
  limit 100;
$$;

create or replace function public.equipment_details(p_equipment_id uuid)
returns table (
  id uuid, owner_id uuid, name text, category text, brand_model text, description text, year int,
  price numeric, price_unit text, approx_lat double precision, approx_lng double precision,
  is_available boolean, owner_first_name text, owner_village text
)
language sql stable security definer set search_path = public as $$
  select e.id, e.owner_id, e.name, e.category, e.brand_model, e.description, e.year,
         e.price, e.price_unit, approximate(e.lat), approximate(e.lng), e.is_available,
         split_part(trim(p.name), ' ', 1), p.village
  from equipment e join profiles p on p.id = e.owner_id
  where auth.uid() is not null
    and e.id = p_equipment_id
    and (e.is_available or e.owner_id = auth.uid());
$$;

-- Busy periods for the availability calendar. No renter details are returned.
create or replace function public.equipment_busy_ranges(p_equipment_id uuid)
returns table (start_at timestamptz, end_at timestamptz)
language sql stable security definer set search_path = public as $$
  select start_at, end_at from equipment_blocked_dates
  where equipment_id = p_equipment_id and auth.uid() is not null
  union all
  select start_at, end_at from bookings
  where equipment_id = p_equipment_id and status = 'accepted' and auth.uid() is not null;
$$;

create or replace function public.rental_cost(p_price numeric, p_price_unit text,
                                              p_start timestamptz, p_end timestamptz)
returns numeric language sql immutable as $$
  select p_price * ceil(extract(epoch from (p_end - p_start)) /
                        case p_price_unit when 'hour' then 3600 else 86400 end);
$$;

create or replace function public.request_booking(p_equipment_id uuid, p_start timestamptz,
                                                  p_end timestamptz, p_message text default '')
returns uuid language plpgsql security definer set search_path = public as $$
declare
  e equipment;
  new_id uuid;
begin
  if auth.uid() is null then raise exception 'not_logged_in'; end if;
  select * into e from equipment where id = p_equipment_id and is_available;
  if not found then raise exception 'equipment_not_found'; end if;
  if e.owner_id = auth.uid() then raise exception 'own_equipment'; end if;
  if p_end <= p_start then raise exception 'end_before_start'; end if;
  if p_start < now() - interval '1 hour' then raise exception 'start_in_past'; end if;
  if exists (select 1 from equipment_busy_ranges(p_equipment_id) r
             where tstzrange(r.start_at, r.end_at) && tstzrange(p_start, p_end)) then
    raise exception 'dates_unavailable';
  end if;

  insert into bookings (equipment_id, renter_id, owner_id, start_at, end_at, estimated_cost, message)
  values (e.id, auth.uid(), e.owner_id, p_start, p_end,
          rental_cost(e.price, e.price_unit, p_start, p_end), coalesce(trim(p_message), ''))
  returning id into new_id;
  return new_id;
end;
$$;

create or replace function public.accept_booking(p_booking_id uuid)
returns void language plpgsql security definer set search_path = public as $$
begin
  update bookings set status = 'accepted'
  where id = p_booking_id and owner_id = auth.uid() and status = 'pending';
  if not found then raise exception 'booking_not_pending'; end if;
exception
  when exclusion_violation then raise exception 'dates_already_booked';
end;
$$;

create or replace function public.reject_booking(p_booking_id uuid)
returns void language plpgsql security definer set search_path = public as $$
begin
  update bookings set status = 'rejected'
  where id = p_booking_id and owner_id = auth.uid() and status = 'pending';
  if not found then raise exception 'booking_not_pending'; end if;
end;
$$;

-- Renters may cancel while pending, or after acceptance until the start time.
create or replace function public.cancel_booking(p_booking_id uuid)
returns void language plpgsql security definer set search_path = public as $$
begin
  update bookings set status = 'cancelled'
  where id = p_booking_id and renter_id = auth.uid()
    and (status = 'pending' or (status = 'accepted' and start_at > now()));
  if not found then raise exception 'booking_not_cancellable'; end if;
end;
$$;

-- Exact location and owner phone, only for the renter of an accepted booking.
create or replace function public.booking_contact(p_booking_id uuid)
returns table (lat double precision, lng double precision, owner_name text, owner_phone text)
language sql stable security definer set search_path = public as $$
  select e.lat, e.lng, p.name, p.phone
  from bookings b
  join equipment e on e.id = b.equipment_id
  join profiles p on p.id = b.owner_id
  where b.id = p_booking_id and b.renter_id = auth.uid() and b.status in ('accepted', 'completed');
$$;

-- Renter name, village and phone for the owner of a booking.
create or replace function public.booking_renter(p_booking_id uuid)
returns table (renter_name text, renter_village text, renter_phone text)
language sql stable security definer set search_path = public as $$
  select p.name, p.village, p.phone
  from bookings b join profiles p on p.id = b.renter_id
  where b.id = p_booking_id and b.owner_id = auth.uid();
$$;

revoke execute on all functions in schema public from public, anon;
grant execute on function
  public.nearby_equipment(double precision, double precision, double precision, text, text),
  public.equipment_details(uuid),
  public.equipment_busy_ranges(uuid),
  public.request_booking(uuid, timestamptz, timestamptz, text),
  public.accept_booking(uuid),
  public.reject_booking(uuid),
  public.cancel_booking(uuid),
  public.booking_contact(uuid),
  public.booking_renter(uuid)
to authenticated;
-- Used inside RLS policies, so logged-in users need them too.
grant execute on function public.owns_equipment(uuid), public.is_listed_equipment(uuid) to authenticated;
