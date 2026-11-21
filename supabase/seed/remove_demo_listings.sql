-- Removes the demo owner and everything they own (listings, blocked dates, bookings).
delete from auth.users where id = 'd0000000-0000-4000-8000-000000000001';
