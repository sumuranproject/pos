-- Bucket privat untuk foto bukti QRIS; file ada di folder <business_id>/<trx>.jpg
insert into storage.buckets (id, name, public) values ('qris-proofs', 'qris-proofs', false)
on conflict (id) do nothing;

create policy qris_proofs_read on storage.objects for select to authenticated
  using (bucket_id = 'qris-proofs' and (storage.foldername(name))[1] = public.current_business()::text);
create policy qris_proofs_write on storage.objects for insert to authenticated
  with check (bucket_id = 'qris-proofs' and (storage.foldername(name))[1] = public.current_business()::text);
create policy qris_proofs_update on storage.objects for update to authenticated
  using (bucket_id = 'qris-proofs' and (storage.foldername(name))[1] = public.current_business()::text);
create policy qris_proofs_delete on storage.objects for delete to authenticated
  using (bucket_id = 'qris-proofs' and (storage.foldername(name))[1] = public.current_business()::text
         and public.current_app_role() = 'owner');
