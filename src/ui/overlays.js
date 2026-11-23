// Sheet bawah, dialog tengah, preview struk, penampil gambar & toast.
// Semua lapisan dirender di SATU <Modal> native (tumpukan View) supaya tidak ada dua Modal yang saling berebut di iOS.
import React, { useRef, useState } from 'react';
import {
  Image, KeyboardAvoidingView, Modal, Platform, Pressable, ScrollView, StyleSheet, TextInput, View, useWindowDimensions,
} from 'react-native';
import { SafeAreaView, useSafeAreaInsets } from 'react-native-safe-area-context';
import * as ImagePicker from 'expo-image-picker';
import * as ImageManipulator from 'expo-image-manipulator';
import {
  closeImageViewer, closeModal, closeReceiptModal, closeSheet, openModal, openReceiptModal, state, toast, tryCloseSomething, useApp,
} from '../lib/store';
import { verifyOwnerPin } from '../lib/auth';
import { transactionReceiptText } from '../lib/receipt';
import { printTransactionReal } from '../lib/printer';
import { Btn, Icon, KbScrollView, R, Row2, T, Muted, useC, Banner, ReceiptText } from './kit';

// ---------- toast ----------
export function Toasts() {
  const { toasts } = useApp();
  const c = useC();
  const insets = useSafeAreaInsets();
  if (!toasts.length) return null;
  return (
    <View pointerEvents="none" style={{ position: 'absolute', left: 16, right: 16, top: insets.top + 8, gap: 6, alignItems: 'center', zIndex: 99 }}>
      {toasts.map((t) => (
        <View key={t.id} style={{ backgroundColor: t.type === 'error' ? c.alert : t.type === 'success' ? c.primary : '#222', paddingHorizontal: 14, paddingVertical: 10, borderRadius: R.md, maxWidth: 480 }}>
          <T size={13} weight="500" color="#fff">{t.msg}</T>
        </View>
      ))}
    </View>
  );
}

// ---------- host ----------
export function OverlayHost() {
  const { sheet, modal, receipt, imageViewer } = useApp();
  const c = useC();
  const { height } = useWindowDimensions();
  const insets = useSafeAreaInsets();
  const visible = Boolean(sheet || modal || receipt || imageViewer);
  const scrollRef = useRef(null);
  return (
    <Modal visible={visible} transparent animationType="fade" statusBarTranslucent onRequestClose={() => { tryCloseSomething(); }}>
      <KeyboardAvoidingView style={{ flex: 1, backgroundColor: sheet ? c.scrim : 'transparent' }} behavior="padding">
        <View style={{ flex: 1 }}>
        {sheet && (
          <View style={[StyleSheet.absoluteFill, { justifyContent: 'flex-end', alignItems: 'center' }]}>
            <Pressable style={StyleSheet.absoluteFill} onPress={closeSheet} />
            <View style={{ width: '100%', maxWidth: 640, maxHeight: '92%', backgroundColor: c.card, borderTopLeftRadius: 20, borderTopRightRadius: 20, paddingTop: 8 }}>
              <View style={{ alignSelf: 'center', width: 40, height: 4, borderRadius: 2, backgroundColor: c.borderStrong, marginBottom: 6 }} />
              <KbScrollView ref={scrollRef} key={sheet.key} keyboardShouldPersistTaps="handled" contentContainerStyle={{ padding: 16, paddingBottom: 24 + insets.bottom }}>
                {sheet.el}
              </KbScrollView>
            </View>
          </View>
        )}
        {modal && <Dialog key={modal.key} onBackdrop={closeModal}>{modal.el}</Dialog>}
        {receipt && <Dialog key={receipt.key} onBackdrop={closeReceiptModal} wide>{receipt.el}</Dialog>}
        {imageViewer && (
          <View style={[StyleSheet.absoluteFill, { backgroundColor: 'rgba(0,0,0,.92)', justifyContent: 'center' }]}>
            <Image source={{ uri: imageViewer }} resizeMode="contain" style={{ width: '100%', height: '80%' }} />
            <Pressable onPress={closeImageViewer} hitSlop={12} style={{ position: 'absolute', top: insets.top + 12, right: 16 }}>
              <Icon name="close" size={30} color="#fff" />
            </Pressable>
          </View>
        )}
        <Toasts />
        </View>
      </KeyboardAvoidingView>
    </Modal>
  );
}

function Dialog({ children, onBackdrop, wide }) {
  const c = useC();
  return (
    <View style={[StyleSheet.absoluteFill, { justifyContent: 'center', alignItems: 'center', padding: 20 }]}>
      <Pressable style={[StyleSheet.absoluteFill, { backgroundColor: c.scrim }]} onPress={onBackdrop} />
      <View style={{ width: '100%', maxWidth: wide ? 380 : 420, backgroundColor: c.card, borderRadius: R.lg, padding: 18, maxHeight: '90%' }}>
        {children}
      </View>
    </View>
  );
}

// ---------- confirm ----------
function ConfirmModal({ title, msg, confirmLabel, onConfirm, danger }) {
  return (
    <>
      <T size={17} weight="700">{title}</T>
      <Muted size={14} style={{ marginTop: 6, marginBottom: 16 }}>{msg}</Muted>
      <Row2>
        <Btn kind="ghost" title="Batal" onPress={closeModal} />
        <Btn kind={danger ? 'danger' : 'primary'} title={confirmLabel} onPress={() => { closeModal(); onConfirm(); }} />
      </Row2>
    </>
  );
}
export function confirmModal(title, msg, confirmLabel, onConfirm, danger = true) {
  openModal(<ConfirmModal title={title} msg={msg} confirmLabel={confirmLabel} onConfirm={onConfirm} danger={danger} />);
}

// ---------- prompt ----------
function PromptModal({ title, label, def, numeric, onConfirm }) {
  const c = useC();
  const [v, setV] = useState(def || '');
  return (
    <>
      <T size={17} weight="700">{title}</T>
      <View style={{ marginVertical: 12, gap: 6 }}>
        <T weight="500">{label}</T>
        <TextInput
          autoFocus value={v} onChangeText={setV} keyboardType={numeric ? 'number-pad' : 'default'}
          style={{ borderWidth: 1, borderColor: c.borderStrong, borderRadius: R.md, paddingHorizontal: 12, minHeight: 48, color: c.text, fontSize: 15, fontFamily: 'inter_regular' }}
        />
      </View>
      <Row2>
        <Btn kind="ghost" title="Batal" onPress={closeModal} />
        <Btn title="Simpan" onPress={() => { closeModal(); onConfirm(v); }} />
      </Row2>
    </>
  );
}
export function promptModal(title, label, def, onConfirm, numeric = false) {
  openModal(<PromptModal title={title} label={label} def={def} numeric={numeric} onConfirm={onConfirm} />);
}

// ---------- PIN owner (diperiksa server lewat RPC verify_owner_pin) ----------
function PinModal({ title, hint, onConfirm }) {
  const c = useC();
  const [pin, setPin] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const check = async (value = pin) => {
    if (value.length < 4) return setError('PIN harus 4 digit');
    setBusy(true);
    let ok = false;
    try { ok = await verifyOwnerPin(value); } catch { setBusy(false); return setError('Tidak bisa memeriksa PIN (offline?).'); }
    setBusy(false);
    if (!ok) { setError('PIN salah.'); setPin(''); return; }
    closeModal();
    onConfirm();
  };
  return (
    <>
      <T size={17} weight="700">{title}</T>
      <Muted size={14} style={{ marginTop: 6 }}>{hint || 'Masukkan PIN owner 4 digit.'}</Muted>
      <TextInput
        autoFocus secureTextEntry keyboardType="number-pad" maxLength={4} value={pin}
        onChangeText={(t) => { const v = t.replace(/\D/g, ''); setPin(v); setError(''); if (v.length === 4) check(v); }}
        style={{ marginVertical: 14, borderWidth: 1, borderColor: c.borderStrong, borderRadius: R.md, textAlign: 'center', fontSize: 28, letterSpacing: 14, minHeight: 56, color: c.text, fontFamily: 'inter_semibold' }}
      />
      {error ? <T size={13} color={c.alert} style={{ marginBottom: 8 }}>{error}</T> : null}
      <Row2>
        <Btn kind="ghost" title="Batal" onPress={closeModal} />
        <Btn title="Konfirmasi" loading={busy} onPress={() => check()} />
      </Row2>
    </>
  );
}
export function pinModal(title, hint, onConfirm) { openModal(<PinModal title={title} hint={hint} onConfirm={onConfirm} />); }

// ---------- bukti QRIS ----------
function QrisCaptureModal({ onConfirm }) {
  return (
    <>
      <T size={17} weight="700">Bukti QRIS</T>
      <Muted size={14} style={{ marginTop: 6, marginBottom: 16 }}>Pastikan pembayaran sudah diterima. Ambil foto bukti QRIS dari layar customer.</Muted>
      <Row2>
        <Btn kind="ghost" title="Batal" onPress={closeModal} />
        <Btn icon="camera" title="Ambil Foto" onPress={() => { closeModal(); onConfirm(); }} />
      </Row2>
    </>
  );
}
export const showQrisCaptureModal = (onConfirm) => openModal(<QrisCaptureModal onConfirm={onConfirm} />);

/** Buka kamera, perkecil (maks 900px, JPEG 70%), kirim { dataUrl, capturedAt, deviceModel, fileSize } ke callback. */
export async function captureQrisProof(callback) {
  try {
    const perm = await ImagePicker.requestCameraPermissionsAsync();
    if (!perm.granted) return toast('Izin kamera ditolak. Aktifkan di Pengaturan.', 'error');
    const res = await ImagePicker.launchCameraAsync({ quality: 0.8, allowsEditing: false });
    if (res.canceled || !res.assets?.[0]) return;
    const a = res.assets[0];
    const capturedAt = Date.now();
    const resize = a.width >= a.height ? { width: Math.min(900, a.width) } : { height: Math.min(900, a.height) };
    const out = await ImageManipulator.manipulateAsync(a.uri, [{ resize }], { compress: 0.7, format: ImageManipulator.SaveFormat.JPEG, base64: true });
    const dataUrl = 'data:image/jpeg;base64,' + out.base64;
    callback({ dataUrl, capturedAt, deviceModel: Platform.OS === 'ios' ? 'iOS device' : 'Android device', fileSize: Math.round(out.base64.length * 0.75) });
  } catch (e) {
    toast('Gagal mengambil foto: ' + (e.message || e), 'error');
  }
}

/** Pilih gambar dari galeri sebagai data URL (untuk gambar QRIS toko). */
export async function pickImageDataUrl(maxSide = 700) {
  const res = await ImagePicker.launchImageLibraryAsync({ mediaTypes: ['images'], quality: 0.8 });
  if (res.canceled || !res.assets?.[0]) return null;
  const a = res.assets[0];
  const resize = a.width >= a.height ? { width: Math.min(maxSide, a.width) } : { height: Math.min(maxSide, a.height) };
  const out = await ImageManipulator.manipulateAsync(a.uri, [{ resize }], { compress: 0.8, format: ImageManipulator.SaveFormat.JPEG, base64: true });
  return 'data:image/jpeg;base64,' + out.base64;
}

// ---------- preview struk ----------
function ReceiptModal({ tx }) {
  const c = useC();
  const [busy, setBusy] = useState(false);
  const print = async () => {
    if (!state.settings.printer.deviceId) { closeReceiptModal(); return toast('Printer belum dipilih · buka Kelola › Printer', 'error'); }
    setBusy(true);
    try { await printTransactionReal(tx); closeReceiptModal(); toast('Struk terkirim ke printer', 'success'); }
    catch (e) { toast('Gagal mencetak: ' + (e.message || e), 'error'); }
    finally { setBusy(false); }
  };
  return (
    <>
      <T size={15} weight="700" style={{ marginBottom: 10 }}>Preview struk</T>
      <ScrollView style={{ maxHeight: 360, backgroundColor: c.surfaceAlt, borderRadius: R.sm }} contentContainerStyle={{ padding: 12 }}>
        <ReceiptText text={transactionReceiptText(tx, state.settings.receipt, state.settings.printer.cols || 32, (() => { const o = (state.outlets || []).find((x) => x.name === tx.outlet) || {}; return { address: o.address, phone: o.phone }; })())} />
      </ScrollView>
      <Row2 style={{ marginTop: 14 }}>
        <Btn kind="ghost" title="Tutup" onPress={closeReceiptModal} />
        <Btn icon="print" title="Cetak" loading={busy} onPress={print} />
      </Row2>
    </>
  );
}
export const printReceipt = (tx) => openReceiptModal(<ReceiptModal tx={tx} />);

export { Banner };
