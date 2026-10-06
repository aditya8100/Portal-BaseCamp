// Focused tests for the agent spec renderer. Run: node --test test/spec.test.mjs
import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { SPEC_VERSION, namesFor, renderSpec } from '../spec.mjs';

describe('agent spec', () => {
  it('exposes a spec version number', () => {
    assert.equal(typeof SPEC_VERSION, 'number');
  });

  it('derives who/other display names from token keys', () => {
    assert.deepEqual(namesFor('alex', ['alex', 'noor']), { who: 'Alex', other: 'Noor' });
    assert.deepEqual(namesFor('noor', ['alex', 'noor']), { who: 'Noor', other: 'Alex' });
    assert.deepEqual(namesFor('solo', ['solo']), { who: 'Solo', other: 'the other' });
  });

  it('renders every placeholder and leaves no template tags', () => {
    const md = renderSpec('# {{WHO}} reads {{OTHER}}\nHi {{WHO}}.', 'Alex', 'Noor');
    assert.equal(md, '# Alex reads Noor\nHi Alex.');
    assert.ok(!md.includes('{{'));
  });
});
