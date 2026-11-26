import React, { useEffect, useState } from 'react';
import { Image, KeyboardAvoidingView, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { showAuthView, state, toast, useApp } from '../lib/store';
import { createBusiness, resetPassword, signIn, signUpOwner } from '../lib/auth';
import { Btn, Field, IconBtn, KbScrollView, LinkBtn, Muted, Select, T, useC } from '../ui/kit';

const Wrap = ({ children, back }) => {
  const c = useC();
  return (
    <SafeAreaView style={{ flex: 1, backgroundColor: c.surface }}>
      <KeyboardAvoidingView style={{ flex: 1 }} behavior="padding">
        <KbScrollView keyboardShouldPersistTaps="handled" contentContainerStyle={{ padding: 24, gap: 16, width: '100%', maxWidth: 460, alignSelf: 'center', flexGrow: 1 }}>
          {back ? <IconBtn name="arrow-back" size={20} onPress={back} style={{ width: 38, height: 38, borderRadius: 19 }} /> : null}
          {children}
        </KbScrollView>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
};

const ErrorText = ({ children }) => { const c = useC(); return children ? <View style={{ backgroundColor: c.alertSoft, borderRadius: 10, paddingHorizontal: 12, paddingVertical: 10 }}><T size={13} color={c.alert}>{children}</T></View> : null; };

function Login() {
  const [user, setUser] = useState('');
  const [pass, setPass] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const submit = async () => {
    setError('');
    if (!user.trim() || !pass) return setError('Email dan password wajib diisi.');
    setLoading(true);
    try { await signIn(user, pass); } catch (ex) { setError(ex.message); } finally { setLoading(false); }
  };
  return (
    <Wrap>
      <View style={{ alignItems: 'center', paddingTop: 24, gap: 8 }}>
        <Image source={require('../../assets/icon.png')} style={{ width: 76, height: 76, borderRadius: 20 }} />
        <T size={26} weight="700">Selamat Datang</T>
        <Muted size={14}>Masuk untuk mengelola bisnis Anda</Muted>
      </View>
      <Field label="Email / Username" icon="mail-outline" placeholder="nama@email.com" value={user} onChangeText={setUser} keyboardType="email-address" />
      <Field label="Password" icon="lock-closed-outline" placeholder="Masukkan password" secure value={pass} onChangeText={setPass} onSubmitEditing={submit} />
      <View style={{ alignItems: 'flex-end' }}><LinkBtn title="Lupa Password?" onPress={() => showAuthView('forgot')} /></View>
      <ErrorText>{error}</ErrorText>
      <Btn title="Masuk" block loading={loading} onPress={submit} />
      <View style={{ flexDirection: 'row', justifyContent: 'center', gap: 4, marginTop: 'auto', paddingTop: 16 }}>
        <Muted size={14}>Belum punya akun?</Muted><LinkBtn title="Daftar Sekarang" onPress={() => showAuthView('register')} />
      </View>
    </Wrap>
  );
}

function Register() {
  const [f, setF] = useState({ name: '', email: '', password: '', password2: '', whatsapp: '' });
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const set = (k) => (v) => setF((p) => ({ ...p, [k]: v }));
  const submit = async () => {
    setError('');
    const name = f.name.trim(); const email = f.email.trim().toLowerCase();
    if (!name || !email || !f.password || !f.password2) return setError('Semua field bertanda * wajib diisi.');
    if (!/^\S+@\S+\.\S+$/.test(email)) return setError('Format email tidak valid.');
    if (f.password.length < 6) return setError('Password minimal 6 karakter.');
    if (f.password !== f.password2) return setError('Konfirmasi password tidak cocok.');
    setBusy(true);
    try {
      const { hasSession } = await signUpOwner({ name, email, password: f.password, whatsapp: f.whatsapp.trim() });
      if (hasSession) { state.pendingRegister = { name, email, whatsapp: f.whatsapp.trim() }; showAuthView('setup'); }
      else { toast('Akun dibuat. Cek email untuk konfirmasi, lalu login.', 'success'); showAuthView('login'); }
    } catch (ex) { setError(ex.message); } finally { setBusy(false); }
  };
  return (
    <Wrap back={() => showAuthView('login')}>
      <T size={28} weight="700" style={{ marginTop: 18 }}>Buat Akun</T>
      <Muted size={14}>Daftarkan bisnis Anda untuk menyimpan data di cloud dan tetap bisa digunakan secara offline.</Muted>
      <Field label="Nama Lengkap / Bisnis *" icon="person-outline" placeholder="Nama Anda atau Bisnis" value={f.name} onChangeText={set('name')} autoCapitalize="words" />
      <Field label="Email *" icon="mail-outline" placeholder="contoh@email.com" value={f.email} onChangeText={set('email')} keyboardType="email-address" />
      <Field label="Password *" icon="lock-closed-outline" placeholder="Minimal 6 karakter" secure value={f.password} onChangeText={set('password')} />
      <Field label="Konfirmasi Password *" icon="lock-closed-outline" placeholder="Ulangi password" secure value={f.password2} onChangeText={set('password2')} />
      <Field label="WhatsApp (Opsional)" icon="logo-whatsapp" placeholder="08123456789" keyboardType="phone-pad" value={f.whatsapp} onChangeText={set('whatsapp')} />
      <ErrorText>{error}</ErrorText>
      <Btn title="Daftar" block loading={busy} onPress={submit} />
      <View style={{ flexDirection: 'row', justifyContent: 'center', gap: 4, marginTop: 'auto', paddingTop: 16 }}>
        <Muted size={14}>Sudah punya akun?</Muted><LinkBtn title="Masuk disini" onPress={() => showAuthView('login')} />
      </View>
    </Wrap>
  );
}

const BUSINESS_TYPES = ['Kafe / Coffee Shop', 'Restoran', 'Warung / Kelontong', 'Retail', 'Jasa', 'Lainnya'];
function Setup() {
  const app = useApp();
  const pending = app.pendingRegister;
  const [f, setF] = useState({ name: '', address: '', phone: '', type: '' });
  const [busy, setBusy] = useState(false);
  useEffect(() => { if (pending) setF((p) => ({ ...p, name: p.name || pending.name })); }, [pending]);
  const set = (k) => (v) => setF((p) => ({ ...p, [k]: v }));
  const submit = async () => {
    if (!state.pendingRegister) { toast('Data registrasi hilang. Silakan daftar ulang.', 'error'); return showAuthView('register'); }
    const name = f.name.trim(); const address = f.address.trim(); const phone = f.phone.trim();
    if (!name || !address || !phone || !f.type) return toast('Lengkapi semua field wajib.', 'error');
    setBusy(true);
    try { await createBusiness({ name, address, phone, type: f.type }); } catch (ex) { toast(ex.message, 'error'); } finally { setBusy(false); }
  };
  return (
    <Wrap back={() => showAuthView('register')}>
      <T size={28} weight="700" style={{ marginTop: 18 }}>Setup Bisnis</T>
      <Muted size={14}>Lengkapi informasi bisnis Anda</Muted>
      <Field label="Nama Bisnis *" placeholder="Nama bisnis" value={f.name} onChangeText={set('name')} autoCapitalize="words" />
      <Field label="Alamat Bisnis *" placeholder="Alamat lengkap" value={f.address} onChangeText={set('address')} autoCapitalize="sentences" />
      <Field label="No Telepon Bisnis *" placeholder="Nomor telepon" keyboardType="phone-pad" value={f.phone} onChangeText={set('phone')} />
      <Field label="Email Bisnis" value={pending?.email || ''} readOnly />
      <Select dropdown icon="storefront-outline" label="Jenis Bisnis *" placeholder="Pilih jenis bisnis" value={f.type} options={BUSINESS_TYPES} onChange={set('type')} />
      <Btn title="Selesai Setup" block loading={busy} onPress={submit} />
    </Wrap>
  );
}

function Forgot() {
  const [email, setEmail] = useState('');
  const [busy, setBusy] = useState(false);
  const [done, setDone] = useState(false);
  const submit = async () => {
    const e = email.trim().toLowerCase();
    if (!/^\S+@\S+\.\S+$/.test(e)) return toast('Format email tidak valid.', 'error');
    setBusy(true);
    try { await resetPassword(e); setDone(true); } catch (ex) { toast(ex.message, 'error'); } finally { setBusy(false); }
  };
  return (
    <Wrap back={() => showAuthView('login')}>
      <T size={28} weight="700" style={{ marginTop: 18 }}>Lupa Password?</T>
      <Muted size={14}>Masukkan email yang terdaftar. Kami akan mengirimkan token untuk mereset password Anda.</Muted>
      {done ? <T size={14}>Tautan reset dikirim ke {email}. Cek inbox Anda.</T> : (
        <>
          <Field label="Email" icon="mail-outline" placeholder="nama@email.com" value={email} onChangeText={setEmail} keyboardType="email-address" />
          <Btn title="Kirim Instruksi" block loading={busy} onPress={submit} />
        </>
      )}
    </Wrap>
  );
}

export default function AuthScreen() {
  const { authView } = useApp();
  if (authView === 'register') return <Register />;
  if (authView === 'setup') return <Setup />;
  if (authView === 'forgot') return <Forgot />;
  return <Login />;
}
