// Logika murni sinkronisasi (tanpa DOM) agar bisa dites dengan `npm test`.
export const ser = (it) => JSON.stringify({ ...it, sync: undefined });

/** Bandingkan snapshot lama (Map id->json) dengan daftar item sekarang → operasi upsert/delete. */
export function computeOps(prev, items) {
  const ops = []; const next = new Map();
  for (const it of items) {
    const id = String(it.id); const json = ser(it);
    next.set(id, json);
    if (prev.get(id) !== json) ops.push({ id, op: 'upsert', item: it });
  }
  for (const id of prev.keys()) if (!next.has(id)) ops.push({ id, op: 'delete' });
  return { ops, next };
}

/** Gabungkan operasi ke outbox: operasi terakhir pada (table,id) menang. */
export function coalesce(outbox, op) {
  return [...outbox.filter((o) => !(o.table === op.table && o.id === op.id)), op];
}
