// Card model: validation + lifecycle rules (zero deps). Importable; no side effects.
export const RECIPE_TTL_MS = 14 * 24 * 3600 * 1000;

const TYPES = new Set(['note', 'recipe', 'list', 'alert', 'mealplan']);
const str = (v, n) => String(v ?? '').slice(0, n);

export function cleanItems(v) {
  if (!Array.isArray(v)) return [];
  return v.slice(0, 50).map((it) => {
    const item = { text: str(it?.text, 200), done: !!it?.done };
    // Structured quantity (optional): enables the in-app scaler. Kept sparse —
    // text-only items carry no extra keys.
    const name = str(it?.name, 100);
    if (name) item.name = name;
    const qty = Number(it?.qty);
    if (it?.qty !== undefined && it?.qty !== null && it?.qty !== '' && Number.isFinite(qty) && qty >= 0) {
      item.qty = qty;
    }
    const unit = str(it?.unit, 20);
    if (unit) item.unit = unit;
    const prep = str(it?.prep, 100);
    if (prep) item.prep = prep;
    if (it?.anchor === true) item.anchor = true;
    return item;
  }).filter((it) => it.text);
}

export function cleanSteps(v) {
  if (!Array.isArray(v)) return [];
  return v.slice(0, 50).map((s) => str(s, 500)).filter(Boolean);
}

export function cleanSlot(s) {
  if (!s || typeof s !== 'object' || Array.isArray(s)) return null;
  const label = str(s.label, 200);
  if (!label) return null;
  const ref = str(s.ref, 100);
  return ref ? { label, ref } : { label };
}

export function cleanPlan(v) {
  if (!Array.isArray(v)) return [];
  return v.slice(0, 7).map((d) => ({
    day: str(d?.day, 10),
    lunch: cleanSlot(d?.lunch),
    dinner: cleanSlot(d?.dinner),
  })).filter((d) => d.day);
}

export function cleanMeta(v) {
  if (!v || typeof v !== 'object' || Array.isArray(v)) return {};
  const out = {};
  for (const [k, val] of Object.entries(v).slice(0, 10)) {
    if (typeof val === 'string' || typeof val === 'number') out[str(k, 30)] = str(val, 100);
  }
  return out;
}

// Validate + whitelist user input. lastCookedAt is server-controlled (see /cooked).
export function cleanCardInput(b, who, nowMs) {
  if (!b || typeof b !== 'object' || Array.isArray(b)) throw new Error('object required');
  const type = b.type === undefined ? 'note' : String(b.type);
  if (!TYPES.has(type)) throw new Error('type must be note|recipe|list|alert|mealplan');
  const title = str(b.title, 200);
  if (!title) throw new Error('title required');
  const image = str(b.image, 2048);
  if (image && !/^https?:\/\//.test(image)) throw new Error('image must be an http(s) URL');
  let expiresAt = null;
  if (b.expiresAt !== undefined && b.expiresAt !== null) {
    expiresAt = Number(b.expiresAt);
    if (!Number.isFinite(expiresAt)) throw new Error('expiresAt must be epoch ms');
  } else if (b.expiresInSec !== undefined && b.expiresInSec !== null) {
    const s = Number(b.expiresInSec);
    if (!Number.isFinite(s) || s <= 0) throw new Error('expiresInSec must be positive');
    expiresAt = nowMs + Math.round(s * 1000);
  }
  const priority = b.priority === undefined ? 0 : Number(b.priority);
  if (!Number.isFinite(priority)) throw new Error('priority must be a number');
  return {
    type,
    title,
    body: str(b.body, 8000),
    items: cleanItems(b.items),
    steps: cleanSteps(b.steps),
    plan: cleanPlan(b.plan),
    meta: cleanMeta(b.meta),
    image,
    source: str(b.source || who, 60),
    priority,
    favorite: !!b.favorite,
    hidden: !!b.hidden,
    pinned: !!b.pinned,
    expiresAt,
  };
}

// Board order: pinned first, then priority, then newest. Missing keys
// (legacy cards) behave as unpinned.
export function compareCards(a, b) {
  return ((b.pinned ? 1 : 0) - (a.pinned ? 1 : 0)) ||
    (b.priority - a.priority) || (b.ts - a.ts);
}

// Lifecycle: explicit expiry wins; non-favorite recipes fade TTL after last
// cook (or posting if never cooked); everything else lives until deleted.
export function isLive(card, nowMs, recipeTtlMs = RECIPE_TTL_MS) {
  if (card.expiresAt && card.expiresAt <= nowMs) return false;
  if (card.type === 'recipe' && !card.favorite) {
    const ref = card.lastCookedAt || card.ts;
    if (nowMs - ref >= recipeTtlMs) return false;
  }
  return true;
}
