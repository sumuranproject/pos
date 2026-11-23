// UI kit SakuKasir untuk React Native: palet tema (sama dengan token CSS versi web) + komponen dasar.
import React from 'react';
import {
  ActivityIndicator, Keyboard, KeyboardAvoidingView, Pressable, ScrollView, StyleSheet, Switch as RNSwitch, Text, TextInput, View, useWindowDimensions,
} from 'react-native';
import { Ionicons, Feather, MaterialCommunityIcons } from '@expo/vector-icons';
import { state, useApp, backToManage } from '../lib/store';
import { formatRupiahInput, parseRupiahInput } from '../lib/format';

export const palettes = {
  light: {
    primary: '#2563EB', primaryHover: '#1D4ED8', primarySoft: '#E8EFFD', onPrimary: '#FFFFFF',
    cash: '#16A34A', cashSoft: '#DCFCE7', qris: '#7C3AED', qrisSoft: '#EDE9FE',
    alert: '#DC2626', alertSoft: '#FEE2E2', warn: '#D97706', warnSoft: '#FEF3C7',
    surface: '#FAFAF7', surfaceAlt: '#F1F0EA', card: '#FFFFFF', border: '#E5E3DA', borderStrong: '#D6D4C9',
    text: '#1A1A1A', muted: '#6B6B63', faint: '#9A9A90', scrim: 'rgba(0,0,0,0.45)',
  },
  dark: {
    primary: '#3B82F6', primaryHover: '#60A5FA', primarySoft: '#14233F', onPrimary: '#FFFFFF',
    cash: '#60A5FA', cashSoft: '#064E3B', qris: '#A78BFA', qrisSoft: '#3B2A63',
    alert: '#F87171', alertSoft: '#4C1D1D', warn: '#FBBF24', warnSoft: '#4A3410',
    surface: '#0F0F0D', surfaceAlt: '#1A1A17', card: '#17171A', border: '#2A2A26', borderStrong: '#3A3A34',
    text: '#F5F5F0', muted: '#A0A09A', faint: '#6E6E68', scrim: 'rgba(0,0,0,0.65)',
  },
};

/** Palet aktif (render ulang otomatis saat tema berubah karena useApp berlangganan store). */
export function useC() { return palettes[useApp().theme] || palettes.light; }
export const cNow = () => palettes[state.theme] || palettes.light;

/** true bila layar lebar (tablet) → grid produk lebih banyak kolom, form dibatasi lebar. */
export function useWide() { const { width, height } = useWindowDimensions(); return Math.min(width, height) >= 600; }
/** Lebar sidebar tablet (lebih sempit di tablet potret kecil). */
export function useSidebarW() { return useWindowDimensions().width >= 900 ? 260 : 210; }

export const R = { sm: 8, md: 12, lg: 14 };

// ---------- teks ----------
// Font: Inter (4 bobot) & JetBrains Mono, ditanam lewat plugin expo-font (app.json). Di Android tiap bobot = satu family.
export const MONO = 'jetbrains_mono_regular';
export const fontFor = (weight) => { const w = String(weight); return w === 'bold' || +w >= 700 ? 'inter_bold' : +w >= 600 ? 'inter_semibold' : +w >= 500 ? 'inter_medium' : 'inter_regular'; };
export function T({ children, size = 14, weight = '400', color, style, ...rest }) {
  const c = useC();
  const st = StyleSheet.flatten(style) || {};
  const family = st.fontFamily === 'monospace' ? MONO : fontFor(weight);
  return <Text {...rest} style={[{ fontSize: size, color: color || c.text }, style, { fontFamily: family }]}>{children}</Text>;
}
export const Muted = ({ children, size = 12, style, ...r }) => { const c = useC(); return <T size={size} color={c.muted} style={style} {...r}>{children}</T>; };

// Peta nama Ionicons -> Feather / MaterialCommunityIcons agar bentuk ikon sama dengan screenshot (set Lucide).
const ICON_MAP = {
  'search': 'f:search', 'sync-outline': 'f:refresh-cw', 'notifications-outline': 'f:bell', 'create-outline': 'f:edit-2',
  'trash-outline': 'f:trash', 'chevron-forward': 'f:chevron-right', 'chevron-back': 'f:arrow-left', 'chevron-down': 'f:chevron-down',
  'settings-outline': 'f:settings', 'cart-outline': 'f:shopping-bag', 'bag-outline': 'f:shopping-bag', 'grid-outline': 'f:layout',
  'bar-chart-outline': 'm:chart-line', 'filter': 'f:filter', 'remove': 'f:minus', 'add': 'f:plus', 'checkmark': 'f:check',
  'mail-outline': 'f:mail', 'lock-closed-outline': 'f:lock', 'eye-outline': 'f:eye', 'eye-off-outline': 'f:eye-off',
  'arrow-forward': 'f:arrow-right', 'print': 'f:printer', 'print-outline': 'f:printer', 'log-out-outline': 'f:log-out',
  'time-outline': 'f:clock', 'camera': 'f:camera', 'document-text-outline': 'f:file-text', 'wallet-outline': 'f:credit-card',
};
export function Icon({ name, size = 20, color, style }) {
  const c = useC();
  const m = ICON_MAP[name] || name;
  const col = color || c.text;
  if (m.startsWith('f:')) return <Feather name={m.slice(2)} size={size} color={col} style={style} />;
  if (m.startsWith('m:')) return <MaterialCommunityIcons name={m.slice(2)} size={size} color={col} style={style} />;
  return <Ionicons name={m} size={size} color={col} style={style} />;
}

// ---------- tombol ----------
export function Btn({ title, onPress, kind = 'primary', block, lg, sm, disabled, loading, icon, style }) {
  const c = useC();
  const bg = { primary: c.primary, ghost: c.card, danger: c.alert, soft: c.primarySoft }[kind];
  const fg = { primary: c.onPrimary, ghost: c.text, danger: '#fff', soft: c.primary }[kind];
  const off = disabled || loading;
  return (
    <Pressable
      onPress={off ? undefined : onPress}
      style={({ pressed }) => [{
        backgroundColor: bg, borderRadius: sm ? 10 : R.md, minHeight: lg ? 52 : sm ? 36 : 44, paddingHorizontal: sm ? 12 : 16,
        alignItems: 'center', justifyContent: 'center', flexDirection: 'row', gap: 8,
        borderWidth: kind === 'ghost' ? 1 : 0, borderColor: c.borderStrong,
        opacity: off ? 0.5 : pressed ? 0.85 : 1, alignSelf: block ? 'stretch' : 'auto', flexGrow: block ? 0 : 0,
      }, style]}
    >
      {loading ? <ActivityIndicator color={fg} /> : icon ? <Icon name={icon} size={sm ? 15 : 18} color={fg} /> : null}
      <T weight="600" size={lg ? 15 : sm ? 13 : 14} color={fg}>{title}</T>
    </Pressable>
  );
}
export const LinkBtn = ({ title, onPress, style }) => { const c = useC(); return <Pressable onPress={onPress}><T weight="600" color={c.primary} style={style}>{title}</T></Pressable>; };

export function IconBtn({ name, onPress, size = 22, badge, badgeColor, style }) {
  const c = useC();
  return (
    <Pressable onPress={onPress} hitSlop={8} style={[{ width: 38, height: 38, borderRadius: 19, alignItems: 'center', justifyContent: 'center', backgroundColor: c.surfaceAlt }, style]}>
      <Icon name={name} size={size} />
      {badge ? <View style={{ position: 'absolute', top: 8, right: 9, width: 8, height: 8, borderRadius: 4, backgroundColor: badgeColor || c.alert }} /> : null}
    </Pressable>
  );
}

/** Baris tombol sejajar (setara .form-row / .form-actions). */
export const Row2 = ({ children, style }) => <View style={[{ flexDirection: 'row', gap: 8 }, style]}>{React.Children.map(children, (ch) => ch && <View style={{ flex: 1 }}>{ch}</View>)}</View>;

// ---------- input ----------
const DOT = '\u2022';
/** Menghitung nilai asli dari teks yang tampil (titik-titik) setelah satu kali pengeditan. */
function applyMaskedEdit(prevReal, newText) {
  const prevLen = prevReal.length;
  let a = 0;
  while (a < prevLen && a < newText.length && newText[a] === DOT) a++;
  let b = 0;
  while (b < prevLen - a && b < newText.length - a && newText[newText.length - 1 - b] === DOT) b++;
  const inserted = newText.slice(a, newText.length - b).split(DOT).join('');
  return prevReal.slice(0, a) + inserted + prevReal.slice(prevLen - b);
}

export function Field({ label, value, onChangeText, placeholder, secure, keyboardType, multiline, editable = true, maxLength, icon, right, autoCapitalize = 'none', style, onSubmitEditing, returnKeyType }) {
  const c = useC();
  const [show, setShow] = React.useState(false);
  // Android menahan huruf terakhir tetap terlihat ~1,5 detik pada secureTextEntry. Supaya langsung jadi titik,
  // kolom sandi memakai teks biasa yang ditampilkan sebagai titik (nilai asli tetap disimpan di state).
  const mask = Boolean(secure) && !show;
  const real = value == null ? '' : String(value);
  const shown = mask ? DOT.repeat(real.length) : real;
  const onChange = mask ? (t) => onChangeText(applyMaskedEdit(real, t)) : onChangeText;
  const kt = mask && !keyboardType ? 'visible-password' : keyboardType;
  return (
    <View style={[{ gap: 6 }, style]}>
      {label ? <T weight="500" size={14}>{label}</T> : null}
      <View style={{ flexDirection: 'row', alignItems: 'center', backgroundColor: c.card, borderWidth: 1, borderColor: c.borderStrong, borderRadius: R.md, paddingHorizontal: 12, minHeight: 48, opacity: editable ? 1 : 0.6 }}>
        {icon ? <Icon name={icon} size={18} color={c.faint} style={{ marginRight: 8 }} /> : null}
        <TextInput
          value={shown} onChangeText={onChange} placeholder={placeholder} placeholderTextColor={c.faint}
          keyboardType={kt} multiline={multiline} editable={editable}
          maxLength={maxLength} autoCapitalize={autoCapitalize} autoCorrect={false}
          autoComplete={secure ? 'off' : undefined} importantForAutofill={secure ? 'no' : undefined}
          onSubmitEditing={onSubmitEditing} returnKeyType={returnKeyType}
          style={{ flex: 1, color: c.text, fontSize: 15, paddingVertical: 10, fontFamily: 'inter_regular' }}
        />
        {secure ? <Pressable onPress={() => setShow((s) => !s)} hitSlop={8}><Icon name={show ? 'eye-off-outline' : 'eye-outline'} size={18} color={c.faint} /></Pressable> : null}
        {right}
      </View>
    </View>
  );
}

/** Input angka rupiah dengan pemisah ribuan. value & onChange bertipe number. */
export function RupiahInput({ value, onChange, label, placeholder = '0' }) {
  return (
    <Field
      label={label} keyboardType="number-pad" placeholder={placeholder}
      value={value ? formatRupiahInput(value) : ''} onChangeText={(t) => onChange(parseRupiahInput(t))}
    />
  );
}

export function NumField({ label, value, onChange, ...r }) {
  return <Field label={label} keyboardType="number-pad" value={String(value ?? '')} onChangeText={(t) => onChange(t.replace(/[^0-9]/g, ''))} {...r} />;
}

/** Pilihan satu dari banyak (chip yang membungkus) — pengganti <select>. */
export function Select({ label, value, options, onChange }) {
  return (
    <View style={{ gap: 6 }}>
      {label ? <T weight="500">{label}</T> : null}
      <View style={{ flexDirection: 'row', flexWrap: 'wrap', gap: 8 }}>
        {options.map((o) => {
          const v = typeof o === 'string' ? o : o.value; const l = typeof o === 'string' ? o : o.label;
          return <Chip key={String(v)} label={l} active={v === value} onPress={() => onChange(v)} />;
        })}
      </View>
    </View>
  );
}

export function Chip({ label, active, onPress }) {
  const c = useC();
  return (
    <Pressable onPress={onPress} style={{ paddingHorizontal: 14, height: 36, borderRadius: 18, alignItems: 'center', justifyContent: 'center', backgroundColor: active ? c.primary : c.card, borderWidth: 1, borderColor: active ? c.primary : c.borderStrong }}>
      <T size={13} weight="500" color={active ? c.onPrimary : c.text}>{label}</T>
    </Pressable>
  );
}

export function Switch({ on, onChange }) {
  const c = useC();
  return <RNSwitch value={!!on} onValueChange={onChange} trackColor={{ true: c.primary, false: c.borderStrong }} thumbColor="#fff" />;
}
export function SwitchRow({ title, sub, on, onChange }) {
  const c = useC();
  return (
    <View style={{ flexDirection: 'row', alignItems: 'center', gap: 12, paddingVertical: 10, borderBottomWidth: StyleSheet.hairlineWidth, borderBottomColor: c.border }}>
      <View style={{ flex: 1 }}><T weight="500">{title}</T>{sub ? <Muted>{sub}</Muted> : null}</View>
      <Switch on={on} onChange={onChange} />
    </View>
  );
}

// ---------- layout ----------
/** ScrollView yang otomatis menggulir kolom yang sedang fokus agar tidak tertutup keyboard. */
export const KbScrollView = React.forwardRef(function KbScrollView({ onScroll, ...p }, fwd) {
  const ref = React.useRef(null);
  const off = React.useRef(0);
  React.useImperativeHandle(fwd, () => ref.current);
  React.useEffect(() => {
    const sub = Keyboard.addListener('keyboardDidShow', (e) => {
      setTimeout(() => {
        const el = TextInput.State.currentlyFocusedInput && TextInput.State.currentlyFocusedInput();
        if (!el || !el.measureInWindow) return;
        el.measureInWindow((x, y, w, h) => {
          const limit = e.endCoordinates.screenY - 24;
          if (y + h > limit && ref.current) ref.current.scrollTo({ y: off.current + (y + h - limit), animated: true });
        });
      }, 150);
    });
    return () => sub.remove();
  }, []);
  return <ScrollView ref={ref} scrollEventThrottle={16} onScroll={(e) => { off.current = e.nativeEvent.contentOffset.y; if (onScroll) onScroll(e); }} {...p} />;
});

export function Page({ children, scroll = true, pad = 16, bottom = 24, style }) {
  const wide = useWide();
  const kids = React.Children.toArray(children);
  const head = kids.length && kids[0].type === PageHead ? kids[0] : null;
  const rest = head ? kids.slice(1) : kids;
  const inner = { padding: pad, paddingTop: head ? 12 : pad, paddingBottom: bottom, gap: 10, width: '100%', maxWidth: wide ? 1100 : undefined, alignSelf: 'center' };
  if (!scroll) return <View style={{ flex: 1 }}>{head}<View style={[{ flex: 1 }, inner, style]}>{rest}</View></View>;
  return (
    <View style={{ flex: 1 }}>
      {head}
      <KeyboardAvoidingView style={{ flex: 1 }} behavior="padding">
        <KbScrollView style={{ flex: 1 }} keyboardShouldPersistTaps="handled" contentContainerStyle={[inner, style]}>{rest}</KbScrollView>
      </KeyboardAvoidingView>
    </View>
  );
}

export function Card({ children, style, onPress }) {
  const c = useC();
  const base = { backgroundColor: c.card, borderRadius: R.lg, borderWidth: 1, borderColor: c.border, padding: 14 };
  return onPress ? <Pressable onPress={onPress} style={[base, style]}>{children}</Pressable> : <View style={[base, style]}>{children}</View>;
}

export function Section({ title, style }) {
  const c = useC();
  return <T size={11} weight="700" color={c.muted} style={[{ textTransform: 'uppercase', letterSpacing: 0.8, marginTop: 8 }, style]}>{title}</T>;
}

/** Judul halaman: bar putih ber-border di ponsel, polos di tablet (seperti screenshot). Tombol di kanan otomatis kecil. */
export function PageHead({ title, sub, back, right, footer }) {
  const c = useC();
  const wide = useWide();
  const rightEl = React.isValidElement(right) && right.type === Btn ? React.cloneElement(right, { sm: true }) : right;
  return (
    <View style={{ backgroundColor: wide ? 'transparent' : c.card, borderBottomWidth: wide ? 0 : 1, borderBottomColor: c.border }}>
      <View style={{ flexDirection: 'row', alignItems: 'center', gap: 10, paddingHorizontal: 16, paddingVertical: wide ? 14 : 10 }}>
        {back ? <IconBtn name="chevron-back" onPress={back === true ? backToManage : back} /> : null}
        <View style={{ flex: 1 }}><T size={20} weight="700">{title}</T>{sub ? <Muted size={12}>{sub}</Muted> : null}</View>
        {rightEl}
      </View>
      {footer ? <View style={{ paddingHorizontal: 16, paddingBottom: 12 }}>{footer}</View> : null}
    </View>
  );
}

export function ListRow({ title, sub, amount, right, onPress, danger, badge, badgeTone = 'muted' }) {
  const c = useC();
  return (
    <Pressable onPress={onPress} disabled={!onPress} style={{ flexDirection: 'row', alignItems: 'center', gap: 12, paddingVertical: 12, paddingHorizontal: 14, backgroundColor: c.card, borderRadius: R.lg, borderWidth: 1, borderColor: c.border }}>
      <View style={{ flex: 1 }}>
        {typeof title === 'string' ? <T weight="600" color={danger ? c.alert : c.text}>{title}</T> : title}
        {sub ? <Muted>{sub}</Muted> : null}
      </View>
      {amount != null ? <T weight="700">{amount}</T> : null}
      {badge != null ? <Pill text={String(badge)} tone={badgeTone} style={{ alignSelf: 'center' }} /> : null}
      {right}
      {onPress && !right ? <Icon name="chevron-forward" size={18} color={c.faint} /> : null}
    </Pressable>
  );
}

export function SummaryRow({ label, value, strong, color, line }) {
  const c = useC();
  return (
    <View style={{ flexDirection: 'row', justifyContent: 'space-between', paddingVertical: strong ? 8 : 6, borderBottomWidth: line ? StyleSheet.hairlineWidth : 0, borderBottomColor: c.border }}>
      <T weight={strong ? '700' : '400'} size={strong ? 16 : 14} color={strong ? undefined : c.muted}>{label}</T>
      <T weight={strong ? '700' : '600'} size={strong ? 16 : 14} color={color}>{value}</T>
    </View>
  );
}

export function Empty({ icon = 'file-tray-outline', title, sub, action }) {
  const c = useC();
  return (
    <View style={{ alignItems: 'center', paddingVertical: 36, gap: 8 }}>
      {icon ? <Icon name={icon} size={52} color={c.faint} /> : null}
      <T weight="600" size={16}>{title}</T>
      {sub ? <Muted size={13} style={{ textAlign: 'center', maxWidth: 280 }}>{sub}</Muted> : null}
      {action}
    </View>
  );
}

export function Pill({ text, tone = 'cash', icon, style }) {
  const c = useC();
  const bg = { cash: c.cashSoft, qris: c.qrisSoft, warn: c.warnSoft, alert: c.alertSoft, muted: c.surfaceAlt, primary: c.primarySoft }[tone];
  const fg = { cash: c.cash, qris: c.qris, warn: c.warn, alert: c.alert, muted: c.muted, primary: c.primary }[tone];
  return <View style={[{ backgroundColor: bg, paddingHorizontal: 7, paddingVertical: 2, borderRadius: 7, alignSelf: 'flex-start' }, style]}><T size={10} weight="700" color={fg} style={{ letterSpacing: 0.4 }}>{(icon ? icon + ' ' : '') + String(text).toUpperCase()}</T></View>;
}

export function Dot({ status }) {
  const c = useC();
  const col = status === 'synced' ? c.cash : status === 'error' ? c.alert : c.warn;
  return <View style={{ width: 9, height: 9, borderRadius: 5, backgroundColor: col }} />;
}

export function Banner({ tone = 'warn', icon = 'alert-circle-outline', children }) {
  const c = useC();
  const bg = { warn: c.warnSoft, alert: c.alertSoft, info: c.surfaceAlt }[tone];
  const fg = { warn: c.warn, alert: c.alert, info: c.muted }[tone];
  return (
    <View style={{ flexDirection: 'row', gap: 10, backgroundColor: bg, borderRadius: R.md, padding: 12, alignItems: 'flex-start' }}>
      {tone === 'info' ? null : <Icon name={icon} size={20} color={fg} />}
      <View style={{ flex: 1 }}>{typeof children === 'string' ? <T size={13} color={fg}>{children}</T> : children}</View>
    </View>
  );
}

/** Kartu angka. Nilai berawalan "Rp" ditampilkan seperti screenshot: label, "RP" kecil, lalu angkanya. */
export function Kpi({ label, value, color, hero, sub, subColor }) {
  const c = useC();
  const m = /^(-?)\s*Rp\s?(.*)$/.exec(String(value));
  if (hero) {
    return (
      <View style={{ backgroundColor: c.card, borderRadius: R.lg, padding: 14, borderWidth: 1, borderColor: c.border }}>
        <T size={12} color={c.muted}>{label}</T>
        <T size={28} weight="700" color={color || c.text} style={{ marginTop: 2 }}>{value}</T>
        {sub ? <T size={13} weight="600" color={subColor || c.cash}>{sub}</T> : null}
      </View>
    );
  }
  return (
    <View style={{ backgroundColor: c.card, borderRadius: R.lg, padding: 12, borderWidth: 1, borderColor: c.border, flexBasis: '47%', flexGrow: 1 }}>
      <T size={12} color={c.muted}>{label}</T>
      {m ? <T size={9} weight="700" color={c.muted} style={{ marginTop: 2 }}>RP</T> : null}
      <T size={18} weight="700" color={color || c.text}>{m ? m[1] + m[2] : value}</T>
      {sub ? <T size={12} color={c.muted}>{sub}</T> : null}
    </View>
  );
}

/** Bar CTA hijau di bawah (keranjang / bayar): jumlah item + total di kiri, tombol transparan di kanan. */
export function CartCta({ count, total, label, onPress, bottom = 12 }) {
  const c = useC();
  return (
    <Pressable onPress={onPress} style={{ position: 'absolute', left: 12, right: 12, bottom, backgroundColor: c.primary, borderRadius: R.lg, paddingVertical: 12, paddingHorizontal: 14, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', elevation: 4, shadowColor: '#000', shadowOpacity: 0.18, shadowRadius: 8, shadowOffset: { width: 0, height: 3 } }}>
      <View><T size={12} color="rgba(255,255,255,.85)">{count} item</T><T size={18} weight="700" color="#fff">{total}</T></View>
      <View style={{ backgroundColor: 'rgba(255,255,255,.2)', borderRadius: 10, paddingHorizontal: 16, paddingVertical: 10 }}><T weight="700" color="#fff">{label}</T></View>
    </Pressable>
  );
}

/** Teks struk monospace: tiap baris tidak dibungkus; mengecil otomatis bila layar sempit. */
export function ReceiptText({ text, size = 12 }) {
  const c = useC();
  return (
    <View>
      {String(text).split('\n').map((ln, i) => (
        <Text key={i} numberOfLines={1} adjustsFontSizeToFit minimumFontScale={0.6} style={{ fontFamily: MONO, fontSize: size, lineHeight: size + 5, color: c.text }}>{ln || ' '}</Text>
      ))}
    </View>
  );
}
