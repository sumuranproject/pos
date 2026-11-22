// UI kit SakuKasir untuk React Native: palet tema (sama dengan token CSS versi web) + komponen dasar.
import React from 'react';
import {
  ActivityIndicator, Pressable, ScrollView, StyleSheet, Switch as RNSwitch, Text, TextInput, View, useWindowDimensions,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { state, useApp, backToManage } from '../lib/store';
import { formatRupiahInput, parseRupiahInput } from '../lib/format';

export const palettes = {
  light: {
    primary: '#0F5132', primaryHover: '#0A3D26', primarySoft: '#E7F0EA', onPrimary: '#FFFFFF',
    cash: '#16A34A', cashSoft: '#DCFCE7', qris: '#7C3AED', qrisSoft: '#EDE9FE',
    alert: '#DC2626', alertSoft: '#FEE2E2', warn: '#D97706', warnSoft: '#FEF3C7',
    surface: '#FAFAF7', surfaceAlt: '#F1F0EA', card: '#FFFFFF', border: '#E5E3DA', borderStrong: '#D6D4C9',
    text: '#1A1A1A', muted: '#6B6B63', faint: '#9A9A90', scrim: 'rgba(0,0,0,0.45)',
  },
  dark: {
    primary: '#22A06B', primaryHover: '#2BB97C', primarySoft: '#0F2A1E', onPrimary: '#FFFFFF',
    cash: '#34D399', cashSoft: '#064E3B', qris: '#A78BFA', qrisSoft: '#3B2A63',
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

export const R = { sm: 8, md: 12, lg: 16 };

// ---------- teks ----------
export function T({ children, size = 14, weight = '400', color, style, ...rest }) {
  const c = useC();
  return <Text {...rest} style={[{ fontSize: size, fontWeight: weight, color: color || c.text }, style]}>{children}</Text>;
}
export const Muted = ({ children, size = 12, style, ...r }) => { const c = useC(); return <T size={size} color={c.muted} style={style} {...r}>{children}</T>; };

export function Icon({ name, size = 20, color, style }) {
  const c = useC();
  return <Ionicons name={name} size={size} color={color || c.text} style={style} />;
}

// ---------- tombol ----------
export function Btn({ title, onPress, kind = 'primary', block, lg, disabled, loading, icon, style }) {
  const c = useC();
  const bg = { primary: c.primary, ghost: c.card, danger: c.alert, soft: c.primarySoft }[kind];
  const fg = { primary: c.onPrimary, ghost: c.text, danger: '#fff', soft: c.primary }[kind];
  const off = disabled || loading;
  return (
    <Pressable
      onPress={off ? undefined : onPress}
      style={({ pressed }) => [{
        backgroundColor: bg, borderRadius: R.md, minHeight: lg ? 52 : 44, paddingHorizontal: 16,
        alignItems: 'center', justifyContent: 'center', flexDirection: 'row', gap: 8,
        borderWidth: kind === 'ghost' ? 1 : 0, borderColor: c.borderStrong,
        opacity: off ? 0.5 : pressed ? 0.85 : 1, alignSelf: block ? 'stretch' : 'auto', flexGrow: block ? 0 : 0,
      }, style]}
    >
      {loading ? <ActivityIndicator color={fg} /> : icon ? <Icon name={icon} size={18} color={fg} /> : null}
      <T weight="600" size={lg ? 15 : 14} color={fg}>{title}</T>
    </Pressable>
  );
}
export const LinkBtn = ({ title, onPress, style }) => { const c = useC(); return <Pressable onPress={onPress}><T weight="600" color={c.primary} style={style}>{title}</T></Pressable>; };

export function IconBtn({ name, onPress, size = 22, badge, style }) {
  const c = useC();
  return (
    <Pressable onPress={onPress} hitSlop={8} style={[{ width: 40, height: 40, borderRadius: 20, alignItems: 'center', justifyContent: 'center', backgroundColor: c.surfaceAlt }, style]}>
      <Icon name={name} size={size} />
      {badge ? <View style={{ position: 'absolute', top: 8, right: 9, width: 8, height: 8, borderRadius: 4, backgroundColor: c.alert }} /> : null}
    </Pressable>
  );
}

/** Baris tombol sejajar (setara .form-row / .form-actions). */
export const Row2 = ({ children, style }) => <View style={[{ flexDirection: 'row', gap: 8 }, style]}>{React.Children.map(children, (ch) => ch && <View style={{ flex: 1 }}>{ch}</View>)}</View>;

// ---------- input ----------
export function Field({ label, value, onChangeText, placeholder, secure, keyboardType, multiline, editable = true, maxLength, icon, right, autoCapitalize = 'none', style, onSubmitEditing, returnKeyType }) {
  const c = useC();
  const [show, setShow] = React.useState(false);
  return (
    <View style={[{ gap: 6 }, style]}>
      {label ? <T weight="500">{label}</T> : null}
      <View style={{ flexDirection: 'row', alignItems: 'center', backgroundColor: c.card, borderWidth: 1, borderColor: c.borderStrong, borderRadius: R.md, paddingHorizontal: 12, minHeight: 48, opacity: editable ? 1 : 0.6 }}>
        {icon ? <Icon name={icon} size={18} color={c.faint} style={{ marginRight: 8 }} /> : null}
        <TextInput
          value={value} onChangeText={onChangeText} placeholder={placeholder} placeholderTextColor={c.faint}
          secureTextEntry={secure && !show} keyboardType={keyboardType} multiline={multiline} editable={editable}
          maxLength={maxLength} autoCapitalize={autoCapitalize} autoCorrect={false}
          onSubmitEditing={onSubmitEditing} returnKeyType={returnKeyType}
          style={{ flex: 1, color: c.text, fontSize: 15, paddingVertical: 10 }}
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
export function Page({ children, scroll = true, pad = 16, bottom = 24, style }) {
  const wide = useWide();
  const inner = { padding: pad, paddingBottom: bottom, gap: 12, width: '100%', maxWidth: wide ? 1100 : undefined, alignSelf: 'center' };
  if (!scroll) return <View style={[{ flex: 1 }, inner, style]}>{children}</View>;
  return <ScrollView style={{ flex: 1 }} keyboardShouldPersistTaps="handled" contentContainerStyle={[inner, style]}>{children}</ScrollView>;
}

export function Card({ children, style, onPress }) {
  const c = useC();
  const base = { backgroundColor: c.card, borderRadius: R.lg, borderWidth: 1, borderColor: c.border, padding: 14 };
  return onPress ? <Pressable onPress={onPress} style={[base, style]}>{children}</Pressable> : <View style={[base, style]}>{children}</View>;
}

export function Section({ title, style }) {
  const c = useC();
  return <T size={12} weight="700" color={c.muted} style={[{ textTransform: 'uppercase', letterSpacing: 0.6, marginTop: 8 }, style]}>{title}</T>;
}

/** Judul halaman Kelola dengan tombol kembali ke menu Kelola. */
export function PageHead({ title, sub, back, right }) {
  return (
    <View style={{ flexDirection: 'row', alignItems: 'center', gap: 10 }}>
      {back ? <IconBtn name="chevron-back" onPress={back === true ? backToManage : back} /> : null}
      <View style={{ flex: 1 }}><T size={20} weight="700">{title}</T>{sub ? <Muted size={13}>{sub}</Muted> : null}</View>
      {right}
    </View>
  );
}

export function ListRow({ title, sub, amount, right, onPress, danger }) {
  const c = useC();
  return (
    <Pressable onPress={onPress} disabled={!onPress} style={{ flexDirection: 'row', alignItems: 'center', gap: 12, paddingVertical: 12, borderBottomWidth: StyleSheet.hairlineWidth, borderBottomColor: c.border }}>
      <View style={{ flex: 1 }}>
        <T weight="600" color={danger ? c.alert : c.text}>{title}</T>
        {sub ? <Muted>{sub}</Muted> : null}
      </View>
      {amount != null ? <T weight="600">{amount}</T> : null}
      {right}
      {onPress && !right ? <Icon name="chevron-forward" size={18} color={c.faint} /> : null}
    </Pressable>
  );
}

export function SummaryRow({ label, value, strong, color }) {
  return (
    <View style={{ flexDirection: 'row', justifyContent: 'space-between', paddingVertical: 5 }}>
      <T weight={strong ? '700' : '400'} size={strong ? 16 : 14}>{label}</T>
      <T weight={strong ? '700' : '500'} size={strong ? 16 : 14} color={color}>{value}</T>
    </View>
  );
}

export function Empty({ icon = 'file-tray-outline', title, sub, action }) {
  const c = useC();
  return (
    <View style={{ alignItems: 'center', paddingVertical: 36, gap: 8 }}>
      <Icon name={icon} size={52} color={c.faint} />
      <T weight="600" size={16}>{title}</T>
      {sub ? <Muted size={13} style={{ textAlign: 'center', maxWidth: 280 }}>{sub}</Muted> : null}
      {action}
    </View>
  );
}

export function Pill({ text, tone = 'cash' }) {
  const c = useC();
  const bg = { cash: c.cashSoft, qris: c.qrisSoft, warn: c.warnSoft, alert: c.alertSoft, muted: c.surfaceAlt }[tone];
  const fg = { cash: c.cash, qris: c.qris, warn: c.warn, alert: c.alert, muted: c.muted }[tone];
  return <View style={{ backgroundColor: bg, paddingHorizontal: 8, paddingVertical: 2, borderRadius: 10, alignSelf: 'flex-start' }}><T size={11} weight="600" color={fg}>{text}</T></View>;
}

export function Dot({ status }) {
  const c = useC();
  const col = status === 'synced' ? c.cash : status === 'error' ? c.alert : c.warn;
  return <View style={{ width: 9, height: 9, borderRadius: 5, backgroundColor: col }} />;
}

export function Banner({ tone = 'warn', icon = 'alert-circle-outline', children }) {
  const c = useC();
  const bg = { warn: c.warnSoft, alert: c.alertSoft, info: c.primarySoft }[tone];
  const fg = { warn: c.warn, alert: c.alert, info: c.primary }[tone];
  return (
    <View style={{ flexDirection: 'row', gap: 10, backgroundColor: bg, borderRadius: R.md, padding: 12, alignItems: 'flex-start' }}>
      <Icon name={icon} size={20} color={fg} />
      <View style={{ flex: 1 }}>{typeof children === 'string' ? <T size={13} color={fg}>{children}</T> : children}</View>
    </View>
  );
}

export function Kpi({ label, value, color, hero, sub }) {
  const c = useC();
  return (
    <View style={{ backgroundColor: hero ? c.primary : c.card, borderRadius: R.lg, padding: 14, borderWidth: hero ? 0 : 1, borderColor: c.border, flex: hero ? undefined : 1, minWidth: hero ? undefined : 140 }}>
      <T size={12} color={hero ? 'rgba(255,255,255,.8)' : c.muted}>{label}</T>
      <T size={hero ? 28 : 18} weight="700" color={hero ? '#fff' : color || c.text} style={{ marginTop: 2 }}>{value}</T>
      {sub ? <T size={12} color={hero ? 'rgba(255,255,255,.85)' : c.muted}>{sub}</T> : null}
    </View>
  );
}
