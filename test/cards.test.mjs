// Focused tests for the card model + lifecycle. Run: node --test test/cards.test.mjs
import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { cleanCardInput, compareCards, isLive, RECIPE_TTL_MS } from '../cards.mjs';

const NOW = Date.UTC(2026, 9, 5, 21, 0, 0);
const DAY = 24 * 3600 * 1000;

describe('cleanCardInput', () => {
  it('defaults type to note and favorite to false', () => {
    const c = cleanCardInput({ title: 'hi' }, 'a', NOW);
    assert.equal(c.type, 'note');
    assert.equal(c.favorite, false);
    assert.equal(c.source, 'a');
  });

  it('rejects bad type, missing title, bad image, bad expiry', () => {
    assert.throws(() => cleanCardInput({ type: 'bogus', title: 'x' }, 'a', NOW));
    assert.throws(() => cleanCardInput({ type: 'note' }, 'a', NOW));
    assert.throws(() => cleanCardInput({ title: 'x', image: 'ftp://y' }, 'a', NOW));
    assert.throws(() => cleanCardInput({ title: 'x', expiresInSec: -1 }, 'a', NOW));
  });

  it('strips unknown fields and ignores client-set lastCookedAt', () => {
    const c = cleanCardInput({ title: 'x', evil: 1, lastCookedAt: 123 }, 'a', NOW);
    assert.ok(!('evil' in c));
    assert.ok(!('lastCookedAt' in c));
  });

  it('converts expiresInSec to expiresAt', () => {
    const c = cleanCardInput({ title: 'x', expiresInSec: 60 }, 'a', NOW);
    assert.equal(c.expiresAt, NOW + 60000);
  });
});

describe('isLive lifecycle', () => {
  const base = { ts: NOW - DAY, expiresAt: null, favorite: false, lastCookedAt: null };

  it('honors explicit expiry for every type', () => {
    assert.equal(isLive({ ...base, type: 'note', expiresAt: NOW - 1 }, NOW), false);
    assert.equal(isLive({ ...base, type: 'note', expiresAt: NOW + 1000 }, NOW), true);
  });

  it('sweeps stale uncooked non-favorite recipes', () => {
    const stale = { ...base, type: 'recipe', ts: NOW - RECIPE_TTL_MS - 1 };
    assert.equal(isLive(stale, NOW), false);
    const fresh = { ...base, type: 'recipe', ts: NOW - DAY };
    assert.equal(isLive(fresh, NOW), true);
  });

  it('recent cooks keep a recipe alive; old cooks do not', () => {
    const old = NOW - RECIPE_TTL_MS - DAY;
    assert.equal(isLive({ ...base, type: 'recipe', ts: old, lastCookedAt: NOW - DAY }, NOW), true);
    assert.equal(isLive({ ...base, type: 'recipe', ts: old, lastCookedAt: old }, NOW), false);
  });

  it('favorites never sweep', () => {
    const ancient = { ...base, type: 'recipe', favorite: true, ts: NOW - 365 * DAY };
    assert.equal(isLive(ancient, NOW), true);
  });

  it('non-recipe types live until deleted or expired', () => {
    const ancient = { ...base, type: 'list', ts: NOW - 365 * DAY };
    assert.equal(isLive(ancient, NOW), true);
    const plan = { ...base, type: 'mealplan', ts: NOW - 365 * DAY };
    assert.equal(isLive(plan, NOW), true);
  });
});

describe('mealplan cards', () => {
  const day = (d, lunch, dinner) => ({ day: d, lunch, dinner });

  it('accepts the mealplan type with a 7-day lunch/dinner plan', () => {
    const plan = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'].map((d) => day(
      d, { label: `${d} lunch`, ref: 'r1' }, { label: `${d} dinner` },
    ));
    const c = cleanCardInput({ type: 'mealplan', title: 'Week', plan }, 'alex', NOW);
    assert.equal(c.type, 'mealplan');
    assert.equal(c.plan.length, 7);
    assert.deepEqual(c.plan[0], { day: 'Mon', lunch: { label: 'Mon lunch', ref: 'r1' }, dinner: { label: 'Mon dinner' } });
  });

  it('coerces bad plan shapes instead of throwing', () => {
    const c = cleanCardInput({ type: 'mealplan', title: 'Week', plan: 'nope' }, 'alex', NOW);
    assert.deepEqual(c.plan, []);
    const c2 = cleanCardInput({
      type: 'mealplan', title: 'Week',
      plan: [
        day('', { label: 'x' }, null),
        day('Mon', { label: '' }, { label: 'D', ref: 42 }),
        day('Tue', 'flat', ['arr']),
        ...Array.from({ length: 10 }, (_, i) => day(`D${i}`, null, null)),
      ],
    }, 'alex', NOW);
    assert.ok(c2.plan.length <= 7);
    assert.ok(c2.plan.every((d) => d.day));
    const mon = c2.plan.find((d) => d.day === 'Mon');
    assert.equal(mon.lunch, null);
    assert.deepEqual(mon.dinner, { label: 'D', ref: '42' });
    const tue = c2.plan.find((d) => d.day === 'Tue');
    assert.equal(tue.lunch, null);
    assert.equal(tue.dinner, null);
  });

  it('other types carry an empty plan', () => {
    const c = cleanCardInput({ type: 'note', title: 'x' }, 'alex', NOW);
    assert.deepEqual(c.plan, []);
  });
});

describe('hidden cards', () => {
  it('defaults hidden to false', () => {
    const c = cleanCardInput({ title: 'x' }, 'alex', NOW);
    assert.equal(c.hidden, false);
  });

  it('preserves hidden true and coerces truthy values', () => {
    assert.equal(cleanCardInput({ title: 'x', hidden: true }, 'alex', NOW).hidden, true);
    assert.equal(cleanCardInput({ title: 'x', hidden: 1 }, 'alex', NOW).hidden, true);
    assert.equal(cleanCardInput({ title: 'x', hidden: 0 }, 'alex', NOW).hidden, false);
  });

  it('hidden does not affect the sweep lifecycle', () => {
    const base = { ts: NOW - DAY, expiresAt: null, favorite: false, lastCookedAt: null };
    assert.equal(isLive({ ...base, type: 'note', hidden: true }, NOW), true);
    assert.equal(isLive({ ...base, type: 'recipe', hidden: true, ts: NOW - RECIPE_TTL_MS - 1 }, NOW), false);
  });
});

describe('pinned cards', () => {
  it('defaults pinned to false and coerces truthy values', () => {
    assert.equal(cleanCardInput({ title: 'x' }, 'alex', NOW).pinned, false);
    assert.equal(cleanCardInput({ title: 'x', pinned: 1 }, 'alex', NOW).pinned, true);
    assert.equal(cleanCardInput({ title: 'x', pinned: 0 }, 'alex', NOW).pinned, false);
  });

  it('sorts pinned first, then priority, then newest', () => {
    const mk = (o) => ({ priority: 0, ts: NOW, ...o });
    const cards = [
      mk({ id: 'a', ts: NOW - 1 }),
      mk({ id: 'b', pinned: true, ts: NOW - 100 }),
      mk({ id: 'c', priority: 5 }),
      mk({ id: 'd', pinned: true, priority: 9 }),
    ];
    assert.deepEqual(cards.sort(compareCards).map((c) => c.id), ['d', 'b', 'c', 'a']);
  });

  it('treats legacy cards without the key as unpinned', () => {
    const cards = [
      { id: 'new', pinned: false, priority: 0, ts: NOW - 1 },
      { id: 'old', priority: 0, ts: NOW },
    ];
    assert.deepEqual(cards.sort(compareCards).map((c) => c.id), ['old', 'new']);
  });
});

describe('structured item quantities', () => {
  it('preserves qty/unit/name/prep/anchor for the scaler', () => {
    const c = cleanCardInput({
      type: 'recipe', title: 'x',
      items: [{ text: 'Paneer - 320 g', name: 'Paneer', qty: 320, unit: 'g', prep: 'finely chopped', anchor: true }],
    }, 'alex', NOW);
    assert.deepEqual(c.items[0], {
      text: 'Paneer - 320 g', done: false, name: 'Paneer', qty: 320, unit: 'g',
      prep: 'finely chopped', anchor: true,
    });
  });

  it('drops bad qty but keeps the text line', () => {
    for (const qty of ['x', NaN, Infinity, -1, '', null, undefined]) {
      const c = cleanCardInput({ type: 'recipe', title: 'x', items: [{ text: 't', qty }] }, 'alex', NOW);
      assert.ok(!('qty' in c.items[0]), `qty ${String(qty)} should be dropped`);
      assert.equal(c.items[0].text, 't');
    }
  });

  it('keeps text-only items sparse and coerces anchor strictly', () => {
    const c = cleanCardInput({ type: 'list', title: 'x', items: [{ text: 'Eggs' }] }, 'alex', NOW);
    assert.deepEqual(c.items[0], { text: 'Eggs', done: false });
    const c2 = cleanCardInput({
      type: 'recipe', title: 'x',
      items: [{ text: 't', qty: 1, unit: 'x', anchor: 'yes' }],
    }, 'alex', NOW);
    assert.ok(!('anchor' in c2.items[0]));
  });
});
