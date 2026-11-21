-- Demo booking requests on YOUR equipment, sent by the demo account, for testing Accept and Reject.
-- Run demo_listings.sql first. Run this in the Supabase SQL editor.
-- For each listing that is not the demo owner's, it creates two pending requests that overlap:
--   tomorrow 9:00 to 12:00 and tomorrow 11:00 to 14:00 (India time).
-- Accepting one must make accepting the other fail with "already booked".

insert into public.bookings (equipment_id, renter_id, owner_id, start_at, end_at, estimated_cost, message)
select e.id,
       'd0000000-0000-4000-8000-000000000001',
       e.owner_id,
       s.start_at,
       s.end_at,
       public.rental_cost(e.price, e.price_unit, s.start_at, s.end_at),
       s.message
from public.equipment e
cross join (
  values
    ((date_trunc('day', now() at time zone 'Asia/Kolkata') + interval '1 day 9 hours') at time zone 'Asia/Kolkata',
     (date_trunc('day', now() at time zone 'Asia/Kolkata') + interval '1 day 12 hours') at time zone 'Asia/Kolkata',
     'Need it for 3 acres of wheat'),
    ((date_trunc('day', now() at time zone 'Asia/Kolkata') + interval '1 day 11 hours') at time zone 'Asia/Kolkata',
     (date_trunc('day', now() at time zone 'Asia/Kolkata') + interval '1 day 14 hours') at time zone 'Asia/Kolkata',
     '')
) as s(start_at, end_at, message)
where e.owner_id <> 'd0000000-0000-4000-8000-000000000001';
