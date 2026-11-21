-- Photo storage. Run fourth.
-- Files live at <owner user id>/<equipment id>/<file name>.

insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('equipment-photos', 'equipment-photos', false, 5242880, array['image/jpeg', 'image/png', 'image/webp'])
on conflict (id) do nothing;

create policy "logged in users read equipment photos" on storage.objects
  for select to authenticated using (bucket_id = 'equipment-photos');

create policy "owners upload to own folder" on storage.objects
  for insert to authenticated
  with check (bucket_id = 'equipment-photos' and (storage.foldername(name))[1] = auth.uid()::text);

create policy "owners update own photos" on storage.objects
  for update to authenticated
  using (bucket_id = 'equipment-photos' and (storage.foldername(name))[1] = auth.uid()::text);

create policy "owners delete own photos" on storage.objects
  for delete to authenticated
  using (bucket_id = 'equipment-photos' and (storage.foldername(name))[1] = auth.uid()::text);
