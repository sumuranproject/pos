// Splash animasi: warna & posisi logo sama dengan splash native (assets/splash.png) supaya peralihannya mulus.
import React, { useEffect, useRef } from 'react';
import { Animated, Easing, StyleSheet, Text, useWindowDimensions } from 'react-native';

const MIN_MS = 900; // tampil minimal, supaya tidak berkedip bila data siap sangat cepat

export default function Splash({ ready, onDone }) {
  const { width } = useWindowDimensions();
  const size = width * (1000 / 2732); // sama dengan ukuran gambar di splash native
  const fade = useRef(new Animated.Value(1)).current;
  const pulse = useRef(new Animated.Value(0)).current;
  const exit = useRef(new Animated.Value(0)).current;
  const label = useRef(new Animated.Value(0)).current;
  const t0 = useRef(Date.now()).current;

  useEffect(() => {
    Animated.timing(label, { toValue: 1, duration: 500, delay: 250, easing: Easing.out(Easing.cubic), useNativeDriver: true }).start();
  }, [label]);

  useEffect(() => {
    const loop = Animated.loop(Animated.sequence([
      Animated.timing(pulse, { toValue: 1, duration: 700, easing: Easing.inOut(Easing.quad), useNativeDriver: true }),
      Animated.timing(pulse, { toValue: 0, duration: 700, easing: Easing.inOut(Easing.quad), useNativeDriver: true }),
    ]));
    loop.start();
    return () => loop.stop();
  }, [pulse]);

  useEffect(() => {
    if (!ready) return undefined;
    const id = setTimeout(() => {
      Animated.parallel([
        Animated.timing(exit, { toValue: 1, duration: 380, easing: Easing.in(Easing.cubic), useNativeDriver: true }),
        Animated.timing(fade, { toValue: 0, duration: 380, useNativeDriver: true }),
      ]).start(({ finished }) => { if (finished) onDone(); });
    }, Math.max(0, MIN_MS - (Date.now() - t0)));
    return () => clearTimeout(id);
  }, [ready, exit, fade, onDone, t0]);

  const scale = Animated.multiply(
    pulse.interpolate({ inputRange: [0, 1], outputRange: [1, 1.05] }),
    exit.interpolate({ inputRange: [0, 1], outputRange: [1, 1.18] }),
  );
  return (
    <Animated.View pointerEvents="auto" style={[StyleSheet.absoluteFill, { backgroundColor: '#2563EB', alignItems: 'center', justifyContent: 'center', opacity: fade }]}>
      <Animated.Image source={require('../../assets/icon.png')} style={{ width: size, height: size, transform: [{ scale }] }} resizeMode="contain" />
      <Animated.View style={{ position: 'absolute', top: '50%', marginTop: size / 2 + 18, alignItems: 'center', opacity: label, transform: [{ translateY: label.interpolate({ inputRange: [0, 1], outputRange: [8, 0] }) }] }}>
        <Text style={{ color: '#fff', fontSize: 26, fontWeight: '700', letterSpacing: 0.3 }}>SakuKasir</Text>
        <Text style={{ color: 'rgba(255,255,255,.75)', fontSize: 13, marginTop: 4 }}>Kasir simpel, tetap jalan saat offline</Text>
      </Animated.View>
    </Animated.View>
  );
}
