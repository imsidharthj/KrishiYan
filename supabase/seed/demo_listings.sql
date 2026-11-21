-- Demo listings for testing. Paste into the Supabase SQL editor and run.
-- Creates one demo owner (who never logs in) with listings around Bengaluru and Kelwara (Baran, Rajasthan).
-- Remove everything again with remove_demo_listings.sql.

insert into auth.users (instance_id, id, aud, role, email, email_confirmed_at,
                        raw_app_meta_data, raw_user_meta_data, created_at, updated_at)
values ('00000000-0000-0000-0000-000000000000', 'd0000000-0000-4000-8000-000000000001',
        'authenticated', 'authenticated', 'demo.owner@krishiyan.test', now(),
        '{"provider":"email","providers":["email"]}', '{}', now(), now())
on conflict (id) do nothing;

insert into public.profiles (id, name, phone, village)
values ('d0000000-0000-4000-8000-000000000001', 'Ramesh Demo', '9000000001', 'Demo village')
on conflict (id) do nothing;

insert into public.equipment
  (id, owner_id, name, category, brand_model, description, year, price, price_unit, lat, lng)
values
  -- Bengaluru
  ('d0000000-0000-4000-8000-000000000101', 'd0000000-0000-4000-8000-000000000001', 'Mahindra 575 tractor', 'tractor',
   'Mahindra 575 DI', '45 HP, good condition. Driver available on request.', 2019, 800, 'hour', 13.1007, 77.5963),
  ('d0000000-0000-4000-8000-000000000102', 'd0000000-0000-4000-8000-000000000001', 'Rotavator 6 feet', 'rotavator',
   'Shaktiman', 'Fits 40 HP and above tractors.', 2021, 1500, 'day', 13.0707, 77.7982),
  ('d0000000-0000-4000-8000-000000000103', 'd0000000-0000-4000-8000-000000000001', 'Battery sprayer', 'sprayer',
   'Neptune 16 L', 'Two batteries included.', null, 200, 'day', 12.9081, 77.4826),
  ('d0000000-0000-4000-8000-000000000104', 'd0000000-0000-4000-8000-000000000001', 'Seed drill 9 row', 'seed_drill',
   'Fieldking', 'For wheat, ragi and pulses.', 2020, 1200, 'day', 13.2437, 77.7120),
  ('d0000000-0000-4000-8000-000000000105', 'd0000000-0000-4000-8000-000000000001', 'Swaraj 744 tractor', 'tractor',
   'Swaraj 744 FE', 'Busy today, free from tomorrow.', 2018, 750, 'hour', 12.7111, 77.6956),
  ('d0000000-0000-4000-8000-000000000106', 'd0000000-0000-4000-8000-000000000001', 'Power tiller', 'cultivator',
   'VST Shakti 130', 'Good for small plots.', 2022, 600, 'hour', 12.9716, 77.5946),
  -- Kelwara, Baran, Rajasthan
  ('d0000000-0000-4000-8000-000000000201', 'd0000000-0000-4000-8000-000000000001', 'John Deere 5050 tractor', 'tractor',
   'John Deere 5050 D', '50 HP with trolley.', 2020, 900, 'hour', 25.1410, 76.8820),
  ('d0000000-0000-4000-8000-000000000202', 'd0000000-0000-4000-8000-000000000001', 'Combine harvester', 'harvester',
   'Preet 987', 'Wheat and soybean harvesting.', 2017, 2200, 'hour', 25.1650, 76.9100),
  ('d0000000-0000-4000-8000-000000000203', 'd0000000-0000-4000-8000-000000000001', 'Thresher', 'thresher',
   'Dashmesh', 'Multi crop thresher.', 2016, 2500, 'day', 25.1100, 76.8500),
  ('d0000000-0000-4000-8000-000000000204', 'd0000000-0000-4000-8000-000000000001', 'Cultivator 9 tyne', 'cultivator',
   'Local make', 'Spring loaded tynes.', null, 700, 'day', 25.1900, 76.8300),
  ('d0000000-0000-4000-8000-000000000205', 'd0000000-0000-4000-8000-000000000001', 'Tractor sprayer 400 L', 'sprayer',
   'Aspee', 'Busy today, free from tomorrow.', 2021, 1800, 'day', 25.0800, 76.9300),
  ('d0000000-0000-4000-8000-000000000206', 'd0000000-0000-4000-8000-000000000001', 'Water tanker', 'other',
   '5000 L', 'For spraying and cattle.', 2015, 1000, 'day', 25.1500, 76.8700)
on conflict (id) do nothing;

-- Two listings are busy today so the "Busy now" label can be checked.
insert into public.equipment_blocked_dates (equipment_id, start_at, end_at)
values
  ('d0000000-0000-4000-8000-000000000105', date_trunc('day', now()), date_trunc('day', now()) + interval '1 day'),
  ('d0000000-0000-4000-8000-000000000205', date_trunc('day', now()), date_trunc('day', now()) + interval '1 day');
