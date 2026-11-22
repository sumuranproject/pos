// Edge Function: owner membuat / mengubah akun kasir (butuh service role, tidak bisa dari klien).
// Deploy: supabase functions deploy manage-worker
// Body: { action: 'create' | 'update', username, password?, displayName, whatsapp?, outlet, permissions: string[], active: boolean, id? }
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2';

const cors = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
};
const json = (body: unknown, status = 200) =>
  new Response(JSON.stringify(body), { status, headers: { ...cors, 'Content-Type': 'application/json' } });

Deno.serve(async (req) => {
  if (req.method === 'OPTIONS') return new Response('ok', { headers: cors });
  try {
    const url = Deno.env.get('SUPABASE_URL')!;
    const admin = createClient(url, Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!);
    const caller = createClient(url, Deno.env.get('SUPABASE_ANON_KEY')!, {
      global: { headers: { Authorization: req.headers.get('Authorization') ?? '' } },
    });

    const { data: u } = await caller.auth.getUser();
    if (!u?.user) return json({ error: 'Tidak terautentikasi' }, 401);
    const { data: me } = await admin.from('profiles').select('business_id, role, active').eq('id', u.user.id).single();
    if (!me || me.role !== 'owner' || !me.active) return json({ error: 'Hanya owner' }, 403);

    const b = await req.json();
    const username = String(b.username ?? '').trim().toLowerCase();
    const email = `${username}@worker.sakukasir.local`;

    if (b.action === 'create') {
      if (!/^[a-z0-9._-]{3,30}$/.test(username)) return json({ error: 'Username 3–30 karakter (huruf kecil, angka, . _ -)' }, 400);
      if (!b.password || String(b.password).length < 6) return json({ error: 'Password minimal 6 karakter' }, 400);
      const { data: created, error } = await admin.auth.admin.createUser({
        email, password: b.password, email_confirm: true, user_metadata: { display_name: b.displayName },
      });
      if (error) return json({ error: error.message.includes('already') ? 'Username sudah dipakai' : error.message }, 400);
      const { error: pe } = await admin.from('profiles').insert({
        id: created.user.id, business_id: me.business_id, role: 'cashier', username,
        display_name: b.displayName, whatsapp: b.whatsapp || null, outlet: b.outlet,
        permissions: b.permissions ?? [], active: b.active ?? true,
      });
      if (pe) { await admin.auth.admin.deleteUser(created.user.id); return json({ error: pe.message }, 400); }
      return json({ ok: true, id: created.user.id });
    }

    if (b.action === 'update') {
      const { data: target } = await admin.from('profiles').select('id, business_id, role').eq('id', b.id).single();
      if (!target || target.business_id !== me.business_id || target.role !== 'cashier') return json({ error: 'Kasir tidak ditemukan' }, 404);
      if (b.password) {
        if (String(b.password).length < 6) return json({ error: 'Password minimal 6 karakter' }, 400);
        const { error } = await admin.auth.admin.updateUserById(b.id, { password: b.password });
        if (error) return json({ error: error.message }, 400);
      }
      const { error } = await admin.from('profiles').update({
        display_name: b.displayName, whatsapp: b.whatsapp || null, outlet: b.outlet,
        permissions: b.permissions ?? [], active: b.active ?? true,
      }).eq('id', b.id);
      if (error) return json({ error: error.message }, 400);
      return json({ ok: true });
    }
    return json({ error: 'action tidak dikenal' }, 400);
  } catch (e) {
    return json({ error: String(e) }, 500);
  }
});
