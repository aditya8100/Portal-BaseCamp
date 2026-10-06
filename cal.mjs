// Minimal .ics parsing + RRULE expansion (zero deps). Importable; no side effects.
export function unfoldIcs(text) {
  const out = [];
  for (const raw of text.split(/\r?\n/)) {
    if (/^[ \t]/.test(raw) && out.length) out[out.length - 1] += raw.slice(1);
    else out.push(raw);
  }
  return out;
}

export function parseProp(line) {
  const ci = line.indexOf(':');
  if (ci < 0) return null;
  const left = line.slice(0, ci);
  const value = line.slice(ci + 1);
  const [name, ...params] = left.split(';');
  const p = {};
  for (const pr of params) {
    const ei = pr.indexOf('=');
    if (ei > 0) p[pr.slice(0, ei).toUpperCase()] = pr.slice(ei + 1);
  }
  return { name: name.toUpperCase(), params: p, value };
}

// Wall-clock in TZ -> epoch ms (single-iteration Intl offset trick).
export function zonedToUtc(y, mo, d, h, mi, s, tz) {
  const guess = Date.UTC(y, mo - 1, d, h, mi, s || 0);
  const parts = Object.fromEntries(
    new Intl.DateTimeFormat('en-US', {
      timeZone: tz, hour12: false, year: 'numeric', month: '2-digit', day: '2-digit',
      hour: '2-digit', minute: '2-digit', second: '2-digit',
    }).formatToParts(new Date(guess)).map((p) => [p.type, p.value]),
  );
  const asUTC = Date.UTC(+parts.year, +parts.month - 1, +parts.day, (+parts.hour) % 24, +parts.minute, +parts.second);
  return guess + (guess - asUTC);
}

export function parseIcsDate(prop, tz) {
  const v = prop.value;
  if (prop.params.VALUE === 'DATE') {
    const y = +v.slice(0, 4), mo = +v.slice(4, 6), d = +v.slice(6, 8);
    return { ms: zonedToUtc(y, mo, d, 0, 0, 0, tz), allDay: true };
  }
  const m = /^(\d{4})(\d{2})(\d{2})T(\d{2})(\d{2})(\d{2})(Z?)$/.exec(v);
  if (!m) return null;
  const [, Y, Mo, D, h, mi, s, z] = m;
  if (z) return { ms: Date.UTC(+Y, +Mo - 1, +D, +h, +mi, +s), allDay: false };
  // NOTE: mo is 1-based here; zonedToUtc does the -1 for Date.UTC itself.
  return { ms: zonedToUtc(+Y, +Mo, +D, +h, +mi, +s, tz), allDay: false }; // floating => home tz
}

export function parseDur(v) {
  const m = /^P(?:(\d+)D)?(?:T(?:(\d+)H)?(?:(\d+)M)?(?:(\d+)S)?)?$/.exec(v || '');
  if (!m) return null;
  return (((+m[1] || 0) * 24 + (+m[2] || 0)) * 60 + (+m[3] || 0)) * 60000 + (+m[4] || 0) * 1000;
}

const WD = { SU: 0, MO: 1, TU: 2, WE: 3, TH: 4, FR: 5, SA: 6 };

export function expandRecurrence(ev, fromMs, toMs, tz) {
  const out = [];
  const rule = {};
  for (const part of (ev.rrule || '').split(';')) {
    const ei = part.indexOf('=');
    if (ei > 0) rule[part.slice(0, ei).toUpperCase()] = part.slice(ei + 1);
  }
  const freq = rule.FREQ;
  if (!['DAILY', 'WEEKLY', 'MONTHLY', 'YEARLY'].includes(freq)) {
    if (ev.start >= fromMs - ev.dur && ev.start <= toMs) out.push(ev.start);
    return out;
  }
  const interval = Math.max(1, parseInt(rule.INTERVAL || '1', 10) || 1);
  const count = rule.COUNT ? parseInt(rule.COUNT, 10) : Infinity;
  let until = Infinity;
  if (rule.UNTIL) {
    const u = parseIcsDate({ params: {}, value: rule.UNTIL }, tz);
    if (u) until = u.ms;
  }
  const byday = rule.BYDAY ? rule.BYDAY.split(',').map((d) => WD[d]).filter((d) => d !== undefined) : null;
  const base = new Date(ev.start);
  const baseDay = base.getUTCDate(), baseMonth = base.getUTCMonth();
  const dayMs = 24 * 3600 * 1000;
  let n = 0;
  if (freq === 'DAILY') {
    for (let t = ev.start, i = 0; t <= Math.min(until, toMs) && i < 1000; t += interval * dayMs, i++) {
      n++;
      if (n > count) break;
      if (t >= fromMs - ev.dur) out.push(t);
    }
    return out;
  }
  if (freq === 'WEEKLY') {
    const days = byday && byday.length ? [...byday].sort() : [new Date(ev.start).getUTCDay()];
    const startD = new Date(ev.start);
    startD.setUTCHours(0, 0, 0, 0);
    const week0 = startD.getTime() - startD.getUTCDay() * dayMs;
    const timeMs = ev.start - startD.getTime();
    for (let w = 0, i = 0; w < 520 && i < 1000; w += interval, i++) {
      for (const d of days) {
        const t = week0 + w * 7 * dayMs + d * dayMs + timeMs;
        if (t < ev.start) continue;
        if (t > Math.min(until, toMs)) return out;
        n++;
        if (n > count) return out;
        if (t >= fromMs - ev.dur) out.push(t);
      }
    }
    return out;
  }
  const step = freq === 'MONTHLY' ? interval : interval * 12;
  for (let m = 0; m < 240; m += step) {
    const t = Date.UTC(base.getUTCFullYear(), baseMonth + m, baseDay,
      base.getUTCHours(), base.getUTCMinutes(), base.getUTCSeconds());
    const chk = new Date(t);
    if (chk.getUTCDate() !== baseDay) continue;
    if (t < ev.start) continue;
    if (t > Math.min(until, toMs)) break;
    n++;
    if (n > count) break;
    if (t >= fromMs - ev.dur) out.push(t);
  }
  return out;
}

const unesc = (v) => v.replace(/\\([,;\\nN])/g, (_, c) => (c === 'n' || c === 'N' ? '\n' : c));

export function parseIcs(text, tz) {
  const lines = unfoldIcs(text);
  const events = [];
  let cur = null;
  for (const line of lines) {
    if (line === 'BEGIN:VEVENT') { cur = { exdates: new Set() }; continue; }
    if (line === 'END:VEVENT') {
      if (cur?.startMs !== undefined) {
        const dur = cur.endMs !== undefined
          ? Math.max(0, cur.endMs - cur.startMs)
          : (cur.durMs ?? 3600000);
        events.push({
          title: cur.summary || '(untitled)', start: cur.startMs, dur,
          allDay: !!cur.allDay, location: cur.location || '', rrule: cur.rrule || '',
          exdates: cur.exdates,
        });
      }
      cur = null;
      continue;
    }
    if (!cur) continue;
    const p = parseProp(line);
    if (!p) continue;
    if (p.name === 'SUMMARY') cur.summary = unesc(p.value);
    else if (p.name === 'LOCATION') cur.location = unesc(p.value);
    else if (p.name === 'DTSTART') { const d = parseIcsDate(p, tz); if (d) { cur.startMs = d.ms; cur.allDay = d.allDay; } }
    else if (p.name === 'DTEND') { const d = parseIcsDate(p, tz); if (d) cur.endMs = d.ms; }
    else if (p.name === 'DURATION') { const d = parseDur(p.value); if (d !== null) cur.durMs = d; }
    else if (p.name === 'RRULE') cur.rrule = p.value;
    else if (p.name === 'EXDATE') {
      for (const piece of p.value.split(',')) {
        const d = parseIcsDate({ params: p.params, value: piece }, tz);
        if (d) cur.exdates.add(d.ms);
      }
    }
  }
  return events;
}

const WD3 = { Sun: 0, Mon: 1, Tue: 2, Wed: 3, Thu: 4, Fri: 5, Sat: 6 };

function tzParts(ms, tz) {
  const p = Object.fromEntries(
    new Intl.DateTimeFormat('en-US', {
      timeZone: tz, weekday: 'short', year: 'numeric', month: '2-digit', day: '2-digit',
      hour: '2-digit', minute: '2-digit', hour12: false,
    }).formatToParts(new Date(ms)).map((x) => [x.type, x.value]),
  );
  return {
    wd: WD3[p.weekday], y: +p.year, mo: +p.month, d: +p.day,
    min: ((+p.hour) % 24) * 60 + (+p.minute),
    key: `${p.year}-${p.month}-${p.day}`,
  };
}

// Sunday-start 7-day view containing nowMs. DST-safe: buckets and minutes are
// computed in wall-clock terms, never by adding 24h to epochs.
export function weekView(events, nowMs, tz) {
  const cur = tzParts(nowMs, tz);
  const sunUTC = Date.UTC(cur.y, cur.mo - 1, cur.d) - cur.wd * 86400000;
  const sun = new Date(sunUTC);
  const days = [];
  for (let i = 0; i < 7; i++) {
    const dt = new Date(sunUTC + i * 86400000);
    const key = `${dt.getUTCFullYear()}-${String(dt.getUTCMonth() + 1).padStart(2, '0')}-${String(dt.getUTCDate()).padStart(2, '0')}`;
    days.push({
      key, dayNum: dt.getUTCDate(),
      label: ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'][dt.getUTCDay()],
      allDay: [], timed: [],
    });
  }
  const byKey = Object.fromEntries(days.map((d) => [d.key, d]));
  const weekStartMs = zonedToUtc(sun.getUTCFullYear(), sun.getUTCMonth() + 1, sun.getUTCDate(), 0, 0, 0, tz);
  const occ = upcoming(events, weekStartMs - 86400000, weekStartMs + 8 * 86400000, weekStartMs, tz, 200);
  for (const o of occ) {
    const a = tzParts(o.start, tz);
    const b = tzParts(o.end, tz);
    const day = byKey[a.key];
    if (!day) continue;
    if (o.allDay) {
      day.allDay.push({ title: o.title, location: o.location });
    } else {
      day.timed.push({
        title: o.title,
        location: o.location,
        startMin: a.key === day.key ? a.min : 0,
        endMin: b.key === day.key ? Math.max(b.min, a.min + 15) : 1440,
      });
    }
  }
  for (const d of days) d.timed.sort((x, y) => x.startMin - y.startMin);
  return { tz, weekStartMs, days };
}

export function upcoming(events, fromMs, toMs, nowMs, tz, limit = 20) {
  const occ = [];
  for (const ev of events) {
    for (const t of expandRecurrence(ev, fromMs, toMs, tz)) {
      if (ev.exdates.has(t) || t + ev.dur < nowMs - 3600000 || t > toMs) continue;
      occ.push({ title: ev.title, start: t, end: t + ev.dur, location: ev.location, allDay: ev.allDay });
    }
  }
  occ.sort((a, b) => a.start - b.start);
  return occ.slice(0, limit);
}
