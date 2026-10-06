// Base Camp board server (zero dependencies).
// Usage: PORT=8091 node board.mjs
// ./tokens.json {partner1,partner2} (chmod 600). ./hb.json {username,password} (owner-created, chmod 600).
// ./config.json {lat, lon, tz, calendarUrl} (chmod 600, contains feed URL).
import { createServer } from 'node:http';
import { readFileSync, writeFileSync, existsSync } from 'node:fs';
import { randomUUID, timingSafeEqual } from 'node:crypto';
import { parseIcs, upcoming, weekView } from './cal.mjs';
import { cleanCardInput, compareCards, isLive } from './cards.mjs';
import { shapeWeather } from './weather.mjs';
import { SPEC_VERSION, namesFor, renderSpec } from './spec.mjs';

const PORT = Number(process.env.PORT || 8091);
const HB_URL = process.env.HB_URL || 'http://192.168.1.139:8581';
const MAX_BODY = 64 * 1024;
const MAX_CARDS = 200;
const HERE = (f) => new URL(`./${f}`, import.meta.url);

const TOKENS = JSON.parse(readFileSync(HERE('tokens.json'), 'utf8'));
const CONFIG = existsSync(HERE('config.json')) ? JSON.parse(readFileSync(HERE('config.json'), 'utf8')) : {};
const TZ = CONFIG.tz || 'America/Chicago';

let cards = existsSync(HERE('cards.json')) ? JSON.parse(readFileSync(HERE('cards.json'), 'utf8')) : [];
if (!Array.isArray(cards)) cards = [];
const save = () => writeFileSync(HERE('cards.json'), JSON.stringify(cards, null, 2));
const now = () => Date.now();
const live = (c) => isLive(c, now());
function sweep() {
  const before = cards.length;
  cards = cards.filter(live).slice(0, MAX_CARDS);
  if (cards.length !== before) save();
}

// ---- Weather (Open-Meteo, keyless) ----
const WMO = new Map([
  [0, 'Clear'], [1, 'Mainly clear'], [2, 'Partly cloudy'], [3, 'Overcast'],
  [45, 'Fog'], [48, 'Icy fog'], [51, 'Light drizzle'], [53, 'Drizzle'], [55, 'Heavy drizzle'],
  [56, 'Freezing drizzle'], [57, 'Freezing drizzle'], [61, 'Light rain'], [63, 'Rain'], [65, 'Heavy rain'],
  [66, 'Freezing rain'], [67, 'Freezing rain'], [71, 'Light snow'], [73, 'Snow'], [75, 'Heavy snow'],
  [77, 'Snow grains'], [80, 'Light showers'], [81, 'Showers'], [82, 'Violent showers'],
  [85, 'Snow showers'], [86, 'Snow showers'], [95, 'Thunderstorm'], [96, 'Storm + hail'], [99, 'Storm + hail'],
]);
let weatherCache = null;
async function getWeather() {
  if (weatherCache && now() - weatherCache.fetchedAt < 15 * 60 * 1000) return weatherCache;
  if (typeof CONFIG.lat !== 'number' || typeof CONFIG.lon !== 'number') {
    return { error: 'weather not configured', stale: false };
  }
  try {
    const u = `https://api.open-meteo.com/v1/forecast?latitude=${CONFIG.lat}&longitude=${CONFIG.lon}` +
      `&current=temperature_2m,weather_code` +
      `&hourly=temperature_2m,precipitation_probability,weather_code` +
      `&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max,sunrise,sunset` +
      `&temperature_unit=celsius&timezone=${encodeURIComponent(TZ)}&forecast_days=8`;
    const r = await fetch(u);
    if (!r.ok) throw new Error(`upstream ${r.status}`);
    const j = await r.json();
    weatherCache = {
      stale: false, fetchedAt: now(),
      ...shapeWeather(j, now(), TZ, j.utc_offset_seconds),
    };
  } catch (e) {
    if (weatherCache) return { ...weatherCache, stale: true };
    return { error: 'weather unavailable', stale: false };
  }
  return weatherCache;
}

// ---- Calendar (.ics feed via cal.mjs) ----
let calCache = null;
async function getCalendar() {
  if (calCache && now() - calCache.fetchedAt < 10 * 60 * 1000) return calCache;
  if (!CONFIG.calendarUrl) return { error: 'calendar not configured', stale: false };
  try {
    const r = await fetch(CONFIG.calendarUrl);
    if (!r.ok) throw new Error(`upstream ${r.status}`);
    const events = parseIcs(await r.text(), TZ);
    const occ = upcoming(events, now() - 6 * 3600 * 1000, now() + 14 * 24 * 3600 * 1000, now(), TZ);
    calCache = { stale: false, fetchedAt: now(), tz: TZ, events: occ };
  } catch (e) {
    if (calCache) return { ...calCache, stale: true };
    return { error: 'calendar unavailable', stale: false };
  }
  return calCache;
}

let weekCache = null;
async function getWeek() {
  if (weekCache && now() - weekCache.fetchedAt < 10 * 60 * 1000) return weekCache;
  if (!CONFIG.calendarUrl) return { error: 'calendar not configured', stale: false };
  try {
    const r = await fetch(CONFIG.calendarUrl);
    if (!r.ok) throw new Error(`upstream ${r.status}`);
    const events = parseIcs(await r.text(), TZ);
    weekCache = { stale: false, fetchedAt: now(), ...weekView(events, now(), TZ) };
  } catch (e) {
    if (weekCache) return { ...weekCache, stale: true };
    return { error: 'calendar unavailable', stale: false };
  }
  return weekCache;
}

// ---- Homebridge proxy (unchanged) ----
let hbToken = null;
function hbCreds() {
  try {
    return JSON.parse(readFileSync(HERE('hb.json'), 'utf8'));
  } catch {
    return null;
  }
}
async function hbLogin() {
  const creds = hbCreds();
  if (!creds?.username || !creds?.password) return null;
  const r = await fetch(`${HB_URL}/api/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: creds.username, password: creds.password }),
  });
  if (!r.ok) return null;
  hbToken = (await r.json()).access_token || null;
  return hbToken;
}
async function hbFetch(path, opts = {}, retry = true) {
  if (!hbToken && !(await hbLogin())) {
    return { status: 503, body: { error: 'homebridge not configured' } };
  }
  let r;
  try {
    r = await fetch(`${HB_URL}${path}`, {
      ...opts,
      headers: { ...(opts.headers || {}), Authorization: `Bearer ${hbToken}` },
    });
  } catch (e) {
    if (!retry) throw e;
    await new Promise((r2) => setTimeout(r2, 500));
    return hbFetch(path, opts, false);
  }
  if (r.status === 401 && retry) {
    hbToken = null;
    if (!(await hbLogin())) return { status: 503, body: { error: 'homebridge login failed' } };
    return hbFetch(path, opts, false);
  }
  const text = await r.text();
  let body;
  try {
    body = JSON.parse(text);
  } catch {
    body = { raw: text.slice(0, 500) };
  }
  return { status: r.status, body };
}
function simplifyAccessory(a) {
  const writable = new Set();
  for (const c of a.serviceCharacteristics || []) {
    if ((c.perms || []).includes('pw')) writable.add(c.type);
  }
  return {
    uniqueId: a.uniqueId,
    name: a.serviceName,
    type: a.humanType || a.type,
    values: a.values || {},
    writable: [...writable],
  };
}

function authed(req) {
  const h = req.headers.authorization || '';
  const m = /^Bearer (.+)$/.exec(h);
  if (!m) return null;
  const got = Buffer.from(m[1]);
  for (const [who, tok] of Object.entries(TOKENS)) {
    const want = Buffer.from(tok);
    if (got.length === want.length && timingSafeEqual(got, want)) return who;
  }
  return null;
}

function readJson(req) {
  return new Promise((resolve, reject) => {
    let size = 0;
    const chunks = [];
    req.on('data', (c) => {
      size += c.length;
      if (size > MAX_BODY) {
        reject(new Error('body too large'));
        req.destroy();
      } else chunks.push(c);
    });
    req.on('end', () => {
      try {
        resolve(chunks.length ? JSON.parse(Buffer.concat(chunks).toString('utf8')) : {});
      } catch (e) {
        reject(e);
      }
    });
    req.on('error', reject);
  });
}

const json = (res, code, obj) => {
  res.writeHead(code, { 'Content-Type': 'application/json' });
  res.end(JSON.stringify(obj));
};

const server = createServer(async (req, res) => {
  try {
    const url = new URL(req.url, 'http://x');

    if (req.method === 'GET' && url.pathname === '/api/health') {
      return json(res, 200, {
        ok: true, cards: cards.filter(live).length,
        homebridge: !!hbCreds(), weather: CONFIG.lat !== undefined, calendar: !!CONFIG.calendarUrl,
      });
    }

    if (req.method === 'GET' && (url.pathname === '/' || url.pathname === '/board')) {
      const t = url.searchParams.get('token') || '';
      const ok = Object.values(TOKENS).some((tok) => {
        const a = Buffer.from(t);
        const b = Buffer.from(tok);
        return a.length === b.length && timingSafeEqual(a, b);
      });
      if (!ok) {
        res.writeHead(401, { 'Content-Type': 'text/plain' });
        return res.end('missing ?token=');
      }
      res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
      return res.end(readFileSync(HERE('board.html')));
    }

    if (!url.pathname.startsWith('/api/')) {
      res.writeHead(404, { 'Content-Type': 'text/plain' });
      return res.end('not found');
    }
    const who = authed(req);
    if (!who) return json(res, 401, { error: 'bad or missing bearer token' });

    if (req.method === 'GET' && url.pathname === '/api/spec') {
      const { who: whoName, other } = namesFor(who, Object.keys(TOKENS));
      const markdown = renderSpec(readFileSync(HERE('agent-spec.md'), 'utf8'), whoName, other);
      return json(res, 200, { specVersion: SPEC_VERSION, who: whoName, markdown });
    }

    if (req.method === 'GET' && url.pathname === '/api/cards') {
      sweep();
      const sorted = cards.filter(live).sort(compareCards);
      return json(res, 200, { cards: sorted });
    }

    if (req.method === 'POST' && url.pathname === '/api/cards') {
      const clean = cleanCardInput(await readJson(req), who, now());
      const card = { id: randomUUID().slice(0, 8), ...clean, lastCookedAt: null, ts: now(), updatedAt: now() };
      cards.unshift(card);
      sweep();
      save();
      return json(res, 201, { card });
    }

    if (req.method === 'GET' && url.pathname.startsWith('/api/cards/')) {
      const id = url.pathname.slice('/api/cards/'.length);
      const card = cards.find((c) => c.id === id);
      if (!card || !live(card)) return json(res, 404, { error: 'no such card' });
      return json(res, 200, { card });
    }

    if (req.method === 'PATCH' && url.pathname.startsWith('/api/cards/')) {
      const id = url.pathname.slice('/api/cards/'.length);
      const card = cards.find((c) => c.id === id);
      if (!card || !live(card)) return json(res, 404, { error: 'no such card' });
      const b = await readJson(req);
      const merged = cleanCardInput({ ...card, ...b, source: b.source ?? card.source }, who, now());
      Object.assign(card, merged, { id: card.id, ts: card.ts, lastCookedAt: card.lastCookedAt, updatedAt: now() });
      sweep();
      save();
      return json(res, 200, { card });
    }

    if (req.method === 'PUT' && /^\/api\/cards\/[^/]+\/items$/.test(url.pathname)) {
      const id = url.pathname.split('/')[3];
      const card = cards.find((c) => c.id === id);
      if (!card || !live(card)) return json(res, 404, { error: 'no such card' });
      const b = await readJson(req);
      const index = Number(b.index);
      if (!Number.isInteger(index) || index < 0 || index >= card.items.length) {
        return json(res, 400, { error: 'index out of range' });
      }
      card.items[index].done = b.done === undefined ? !card.items[index].done : !!b.done;
      card.updatedAt = now();
      save();
      return json(res, 200, { card });
    }

    if (req.method === 'POST' && /^\/api\/cards\/[^/]+\/cooked$/.test(url.pathname)) {
      const id = url.pathname.split('/')[3];
      const card = cards.find((c) => c.id === id);
      if (!card || !live(card)) return json(res, 404, { error: 'no such card' });
      card.lastCookedAt = now();
      card.updatedAt = now();
      save();
      return json(res, 200, { card });
    }

    if (req.method === 'DELETE' && url.pathname.startsWith('/api/cards/')) {
      const id = url.pathname.slice('/api/cards/'.length);
      const before = cards.length;
      cards = cards.filter((c) => c.id !== id);
      if (cards.length === before) return json(res, 404, { error: 'no such card' });
      save();
      return json(res, 200, { deleted: id });
    }

    if (req.method === 'GET' && url.pathname === '/api/weather') {
      return json(res, 200, await getWeather());
    }

    if (req.method === 'GET' && url.pathname === '/api/calendar') {
      return json(res, 200, await getCalendar());
    }

    if (req.method === 'GET' && url.pathname === '/api/calendar/week') {
      return json(res, 200, await getWeek());
    }

    if (req.method === 'GET' && url.pathname === '/api/home/accessories') {
      const r = await hbFetch('/api/accessories');
      if (r.status !== 200 || !Array.isArray(r.body)) return json(res, r.status, r.body);
      return json(res, 200, { accessories: r.body.map(simplifyAccessory) });
    }

    if (req.method === 'PUT' && url.pathname.startsWith('/api/home/accessories/')) {
      const id = url.pathname.slice('/api/home/accessories/'.length);
      const body = await readJson(req);
      if (!body.characteristicType || body.value === undefined) {
        return json(res, 400, { error: 'characteristicType and value required' });
      }
      const r = await hbFetch(`/api/accessories/${id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ characteristicType: body.characteristicType, value: body.value }),
      });
      return json(res, r.status, r.body);
    }

    return json(res, 404, { error: 'not found' });
  } catch (e) {
    console.error('request failed:', e.message, '| cause:', e.cause ? String(e.cause) : 'none');
    return json(res, 400, { error: String(e.message || e) });
  }
});

server.listen(PORT, '0.0.0.0', () => {
  console.log(`board on http://0.0.0.0:${PORT} (${cards.filter(live).length} cards)`);
});
