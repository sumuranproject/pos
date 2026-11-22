-- Jalankan HANYA bila 0001 sebelumnya sudah sempat berhasil sebagian. Normalnya cukup jalankan 0001/setup-all.sql versi baru.
create extension if not exists pgcrypto with schema extensions;
alter function public.verify_owner_pin(text) set search_path = public, extensions;
alter function public.set_owner_pin(text) set search_path = public, extensions;
-- create_business ikut memakai crypt(): ubah juga (nama argumen mengikuti definisi Anda)
do $$ declare r record; begin
  for r in select oid::regprocedure as sig from pg_proc where pronamespace='public'::regnamespace and proname='create_business'
  loop execute format('alter function %s set search_path = public, extensions', r.sig); end loop;
end $$;
