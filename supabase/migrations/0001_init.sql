-- SakuKasir — skema Supabase (Postgres + RLS)
-- Jalankan di SQL Editor Supabase atau `supabase db push`.
-- Desain: satu bisnis (tenant) per owner; kasir = akun Auth biasa yang terhubung ke bisnis lewat tabel profiles.
-- Koleksi data (produk, transaksi, dst.) disimpan sebagai dokumen JSONB supaya cocok dengan sinkronisasi offline-first
-- (klien membuat id sendiri, upsert idempoten).

create extension if not exists pgcrypto;

-- ---------- tabel inti ----------
create table public.businesses (
  id uuid primary key default gen_random_uuid(),
  owner_id uuid not null references auth.users(id) on delete cascade,
  name text not null,
  address text,
  phone text,
  email text,
  type text,
  created_at timestamptz not null default now()
);

create table public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  business_id uuid not null references public.businesses(id) on delete cascade,
  role text not null check (role in ('owner', 'cashier')),
  username text not null,
  display_name text not null,
  whatsapp text,
  outlet text,
  permissions text[] not null default '{}',
  active boolean not null default true,
  created_at timestamptz not null default now()
);
create unique index profiles_username_key on public.profiles (lower(username));

create table public.business_secrets (
  business_id uuid primary key references public.businesses(id) on delete cascade,
  owner_pin_hash text not null
);

create table public.settings (
  business_id uuid primary key references public.businesses(id) on delete cascade,
  data jsonb not null default '{}'::jsonb,
  updated_at timestamptz not null default now()
);

-- koleksi dokumen
do $$
declare t text;
begin
  foreach t in array array['outlets','categories','products','transactions','expenses','shifts','audit_log','notifications'] loop
    execute format($f$create table public.%I (
      id text not null,
      business_id uuid not null references public.businesses(id) on delete cascade,
      data jsonb not null,
      updated_at timestamptz not null default now(),
      primary key (business_id, id)
    )$f$, t);
    execute format('create index %I on public.%I (business_id, updated_at desc)', t || '_updated_idx', t);
  end loop;
end $$;

-- ---------- fungsi bantu ----------
create or replace function public.current_business() returns uuid
language sql stable security definer set search_path = public as $$
  select business_id from public.profiles where id = auth.uid() and active
$$;
create or replace function public.current_app_role() returns text
language sql stable security definer set search_path = public as $$
  select role from public.profiles where id = auth.uid() and active
$$;
create or replace function public.current_username() returns text
language sql stable security definer set search_path = public as $$
  select username from public.profiles where id = auth.uid() and active
$$;

-- ---------- RLS ----------
alter table public.businesses enable row level security;
alter table public.profiles enable row level security;
alter table public.business_secrets enable row level security; -- tanpa policy: hanya lewat fungsi
alter table public.settings enable row level security;

create policy biz_read on public.businesses for select using (id = public.current_business());
create policy biz_update on public.businesses for update
  using (id = public.current_business() and public.current_app_role() = 'owner');

create policy profiles_read on public.profiles for select
  using (business_id = public.current_business() or id = auth.uid());
create policy profiles_update_self on public.profiles for update using (id = auth.uid());
-- insert/delete profil hanya lewat fungsi security definer / edge function (service role)

create policy settings_read on public.settings for select using (business_id = public.current_business());
create policy settings_write on public.settings for all
  using (business_id = public.current_business() and public.current_app_role() = 'owner')
  with check (business_id = public.current_business() and public.current_app_role() = 'owner');

-- tabel koleksi: baca semua anggota; tulis sesuai peran
do $$
declare t text;
begin
  foreach t in array array['outlets','categories','products','transactions','expenses','shifts','audit_log','notifications'] loop
    execute format('alter table public.%I enable row level security', t);
  end loop;

  -- hanya owner yang boleh mengubah outlet & kategori
  foreach t in array array['outlets','categories'] loop
    execute format('create policy %I on public.%I for select using (business_id = public.current_business())', t||'_r', t);
    execute format($p$create policy %I on public.%I for all
      using (business_id = public.current_business() and public.current_app_role() = 'owner')
      with check (business_id = public.current_business() and public.current_app_role() = 'owner')$p$, t||'_w', t);
  end loop;

  -- produk: semua anggota baca & ubah (stok berkurang saat jual); tambah/hapus hanya owner
  create policy products_r on public.products for select using (business_id = public.current_business());
  create policy products_u on public.products for update
    using (business_id = public.current_business()) with check (business_id = public.current_business());
  create policy products_i on public.products for insert
    with check (business_id = public.current_business() and public.current_app_role() = 'owner');
  create policy products_d on public.products for delete
    using (business_id = public.current_business() and public.current_app_role() = 'owner');

  -- pengeluaran, shift, audit, notifikasi: semua anggota baca/tulis; hapus hanya owner
  foreach t in array array['expenses','shifts','audit_log','notifications'] loop
    execute format('create policy %I on public.%I for select using (business_id = public.current_business())', t||'_r', t);
    execute format('create policy %I on public.%I for insert with check (business_id = public.current_business())', t||'_i', t);
    execute format('create policy %I on public.%I for update using (business_id = public.current_business()) with check (business_id = public.current_business())', t||'_u', t);
    execute format($p$create policy %I on public.%I for delete
      using (business_id = public.current_business() and public.current_app_role() = 'owner')$p$, t||'_d', t);
  end loop;
end $$;

-- transaksi: owner lihat semua; kasir hanya miliknya sendiri
create policy tx_r on public.transactions for select using (
  business_id = public.current_business()
  and (public.current_app_role() = 'owner' or data->>'cashierId' = public.current_username()));
create policy tx_i on public.transactions for insert with check (
  business_id = public.current_business()
  and (public.current_app_role() = 'owner' or data->>'cashierId' = public.current_username()));
create policy tx_u on public.transactions for update
  using (business_id = public.current_business()
    and (public.current_app_role() = 'owner' or data->>'cashierId' = public.current_username()))
  with check (business_id = public.current_business()
    and (public.current_app_role() = 'owner' or data->>'cashierId' = public.current_username()));
create policy tx_d on public.transactions for delete
  using (business_id = public.current_business() and public.current_app_role() = 'owner');

-- ---------- trigger pengaman ----------
-- void hanya boleh dilakukan owner
create or replace function public.guard_tx_void() returns trigger
language plpgsql security definer set search_path = public as $$
begin
  if new.data->>'status' = 'void'
     and coalesce(old.data->>'status', '') <> 'void'
     and public.current_app_role() is distinct from 'owner' then
    raise exception 'Hanya owner yang boleh void transaksi';
  end if;
  return new;
end $$;
create trigger tx_guard_void before update on public.transactions
  for each row execute function public.guard_tx_void();

-- kasir tidak boleh mengubah role/bisnis/izin/status aktif/username miliknya
create or replace function public.guard_profile_update() returns trigger
language plpgsql security definer set search_path = public as $$
begin
  if public.current_app_role() is distinct from 'owner' and (
       new.role <> old.role or new.business_id <> old.business_id or new.permissions <> old.permissions
       or new.active <> old.active or new.username <> old.username or new.outlet is distinct from old.outlet) then
    raise exception 'Perubahan tidak diizinkan';
  end if;
  return new;
end $$;
create trigger profiles_guard before update on public.profiles
  for each row execute function public.guard_profile_update();

-- updated_at otomatis
create or replace function public.touch_updated_at() returns trigger
language plpgsql as $$ begin new.updated_at = now(); return new; end $$;
do $$
declare t text;
begin
  foreach t in array array['outlets','categories','products','transactions','expenses','shifts','audit_log','notifications','settings'] loop
    execute format('create trigger %I before update on public.%I for each row execute function public.touch_updated_at()', t||'_touch', t);
  end loop;
end $$;

-- ---------- RPC ----------
-- Dipanggil setelah owner mendaftar: membuat bisnis, profil owner, outlet utama, kategori awal, pengaturan, PIN default 1234.
create or replace function public.create_business(
  p_name text, p_address text, p_phone text, p_type text,
  p_display_name text, p_whatsapp text default null
) returns uuid
language plpgsql security definer set search_path = public as $$
declare
  uid uuid := auth.uid();
  uemail text;
  bid uuid;
begin
  if uid is null then raise exception 'Belum login'; end if;
  if exists (select 1 from public.profiles where id = uid) then raise exception 'Akun sudah punya bisnis'; end if;
  select email into uemail from auth.users where id = uid;

  insert into public.businesses (owner_id, name, address, phone, email, type)
    values (uid, p_name, p_address, p_phone, uemail, p_type) returning id into bid;
  insert into public.profiles (id, business_id, role, username, display_name, whatsapp, permissions)
    values (uid, bid, 'owner', uemail, p_display_name, p_whatsapp, '{}');
  insert into public.outlets (id, business_id, data)
    values ('1', bid, jsonb_build_object('id', 1, 'name', p_name, 'address', p_address, 'phone', p_phone, 'active', true));
  insert into public.categories (id, business_id, data)
    select (n)::text, bid, jsonb_build_object('id', n, 'name', nm, 'active', true)
    from (values (1,'Makanan'),(2,'Minuman'),(3,'Snack'),(4,'Lainnya')) as v(n, nm);
  insert into public.settings (business_id, data) values (bid, jsonb_build_object(
    'qris', jsonb_build_object('image', null, 'active', true, 'outlet', p_name, 'retentionDays', 35),
    'receipt', jsonb_build_object('bizName', p_name, 'showOutlet', true, 'showTxNumber', true, 'showCashier', true,
                                  'showPayment', true, 'showChange', true, 'footer', 'Terima kasih sudah berbelanja'),
    'notifications', jsonb_build_object('lowStock', true, 'outOfStock', true, 'system', true),
    'security', jsonb_build_object('voidWindowMinutes', 5, 'voidLimitCashier', 50000, 'refundLimitCashier', 50000, 'alertVoidPerDay', 5)));
  insert into public.business_secrets (business_id, owner_pin_hash) values (bid, crypt('1234', gen_salt('bf')));
  return bid;
end $$;

create or replace function public.verify_owner_pin(pin text) returns boolean
language sql stable security definer set search_path = public as $$
  select exists (
    select 1 from public.business_secrets s
    where s.business_id = public.current_business() and s.owner_pin_hash = crypt(pin, s.owner_pin_hash))
$$;

create or replace function public.set_owner_pin(new_pin text) returns void
language plpgsql security definer set search_path = public as $$
begin
  if public.current_app_role() is distinct from 'owner' then raise exception 'Hanya owner'; end if;
  if new_pin !~ '^\d{4}$' then raise exception 'PIN harus 4 digit'; end if;
  update public.business_secrets set owner_pin_hash = crypt(new_pin, gen_salt('bf'))
   where business_id = public.current_business();
end $$;

grant execute on function public.create_business(text,text,text,text,text,text) to authenticated;
grant execute on function public.verify_owner_pin(text) to authenticated;
grant execute on function public.set_owner_pin(text) to authenticated;
