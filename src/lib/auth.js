// Autentikasi & operasi akun lewat Supabase Auth / RPC / Edge Function yang sudah ada (tanpa mode demo).
import { commit, enterApp, showAuthView, state } from './store';
import { supabase, workerEmail } from './supabase';
import { loadWorkers, startSync, stopSync } from './sync';

const err = (m) => new Error(m);

function mapAuthError(e) {
  const m = e?.message || String(e);
  if (/invalid login/i.test(m)) return 'Email/username atau password salah.';
  if (/already registered|already been registered/i.test(m)) return 'Email sudah terdaftar. Silakan login.';
  if (/email not confirmed/i.test(m)) return 'Email belum dikonfirmasi. Cek inbox Anda.';
  if (/fetch|network/i.test(m)) return 'Tidak ada koneksi internet.';
  return m;
}

// ---------------- sesi ----------------
/** Muat profil + data bisnis setelah ada sesi Supabase. Mengembalikan 'app' atau 'setup'. */
export async function bootSession(authUser) {
  const { data: profile, error } = await supabase.from('profiles').select('*').eq('id', authUser.id).maybeSingle();
  if (error) throw error;
  if (!profile) {
    const meta = authUser.user_metadata || {};
    state.pendingRegister = { name: meta.display_name || '', email: authUser.email, whatsapp: meta.whatsapp || '', fromSession: true };
    showAuthView('setup');
    return 'setup';
  }
  if (!profile.active) { await supabase.auth.signOut(); throw err('Akun dinonaktifkan. Hubungi owner.'); }

  const { data: biz } = await supabase.from('businesses').select('*').eq('id', profile.business_id).single();
  state.user = {
    id: profile.id, username: profile.username, displayName: profile.display_name, role: profile.role,
    email: authUser.email, whatsapp: profile.whatsapp || '',
    outlet: profile.role === 'cashier' ? profile.outlet : undefined,
    permissions: profile.permissions || [],
  };
  await startSync(profile.business_id, profile.id);
  state.outlet = state.outlets.find((o) => o.name === profile.outlet) || state.outlets[0] || { id: 0, name: biz?.name || '' };
  if (!state.settings.receipt.bizName) state.settings.receipt.bizName = biz?.name || '';
  enterApp();
  return 'app';
}

export async function restoreSession() {
  const { data } = await supabase.auth.getSession();
  if (data.session) { try { await bootSession(data.session.user); } catch { /* biarkan di layar login */ } }
}

export async function signIn(identifier, password) {
  const id = identifier.trim();
  const email = id.includes('@') ? id.toLowerCase() : workerEmail(id);
  const { data, error } = await supabase.auth.signInWithPassword({ email, password });
  if (error) throw err(mapAuthError(error));
  try { await bootSession(data.user); } catch (e) { throw err(mapAuthError(e)); }
}

export async function signUpOwner({ name, email, password, whatsapp }) {
  const { data, error } = await supabase.auth.signUp({ email, password, options: { data: { display_name: name, whatsapp } } });
  if (error) throw err(mapAuthError(error));
  // identities kosong = email sudah terdaftar (Supabase tidak mengembalikan error demi privasi)
  if (data.user && data.user.identities && data.user.identities.length === 0) throw err('Email sudah terdaftar. Silakan login.');
  return { hasSession: Boolean(data.session) };
}

export async function createBusiness({ name, address, phone, type }) {
  const p = state.pendingRegister;
  const { error } = await supabase.rpc('create_business', {
    p_name: name, p_address: address, p_phone: phone, p_type: type, p_display_name: p?.name || name, p_whatsapp: p?.whatsapp || null,
  });
  if (error) throw err(mapAuthError(error));
  const { data } = await supabase.auth.getUser();
  state.pendingRegister = null;
  await bootSession(data.user);
}

export async function resetPassword(email) {
  const { error } = await supabase.auth.resetPasswordForEmail(email);
  if (error) throw err(mapAuthError(error));
}

export async function signOut() {
  await stopSync(true).catch(() => {});
  await supabase.auth.signOut().catch(() => {});
}

// ---------------- profil ----------------
export async function saveProfile({ displayName, whatsapp }) {
  const { error } = await supabase.from('profiles').update({ display_name: displayName, whatsapp: whatsapp || null }).eq('id', state.user.id);
  if (error) throw err(error.message);
}

export async function changePassword(oldPassword, newPassword) {
  const { error: e1 } = await supabase.auth.signInWithPassword({ email: state.user.email, password: oldPassword });
  if (e1) throw err('Password lama salah.');
  const { error } = await supabase.auth.updateUser({ password: newPassword });
  if (error) throw err(mapAuthError(error));
}

// ---------------- PIN owner ----------------
export async function verifyOwnerPin(pin) {
  const { data, error } = await supabase.rpc('verify_owner_pin', { pin });
  if (error) throw err(error.message);
  return data === true;
}
export async function saveOwnerPin(pin) {
  const { error } = await supabase.rpc('set_owner_pin', { new_pin: pin });
  if (error) throw err(error.message);
}

// ---------------- kasir (via Edge Function) ----------------
export async function saveWorker(action, w) {
  const { data, error } = await supabase.functions.invoke('manage-worker', { body: { action, ...w } });
  if (error) {
    let msg = error.message;
    try { const j = await error.context?.json?.(); if (j?.error) msg = j.error; } catch { /* abaikan */ }
    throw err(msg);
  }
  if (data?.error) throw err(data.error);
  await loadWorkers();
  commit();
}

