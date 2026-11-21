-- My rentals for the renter. Run after 001 to 004.
-- Renters cannot read the equipment table (it holds the exact location),
-- so this returns their bookings with only the equipment name and first photo.

create or replace function public.my_rentals()
returns table (
  id uuid, equipment_id uuid, renter_id uuid, owner_id uuid,
  start_at timestamptz, end_at timestamptz, estimated_cost numeric, message text, status text,
  equipment_name text, photo_path text
)
language sql stable security definer set search_path = public as $$
  select b.id, b.equipment_id, b.renter_id, b.owner_id, b.start_at, b.end_at, b.estimated_cost, b.message, b.status,
         e.name,
         (select storage_path from equipment_images i where i.equipment_id = e.id order by position limit 1)
  from bookings b join equipment e on e.id = b.equipment_id
  where b.renter_id = auth.uid()
  order by b.start_at desc;
$$;

revoke execute on function public.my_rentals() from public, anon;
grant execute on function public.my_rentals() to authenticated;

-- Make the API see the new function straight away.
notify pgrst, 'reload schema';
