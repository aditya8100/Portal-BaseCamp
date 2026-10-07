// Agent spec rendering (zero deps). Importable; no side effects.
export const SPEC_VERSION = 4;

const cap = (s) => (s ? s[0].toUpperCase() + s.slice(1) : s);

export function namesFor(who, allKeys) {
  const others = (allKeys || []).filter((k) => k !== who).map(cap);
  return { who: cap(who), other: others.join(' & ') || 'the other' };
}

export function renderSpec(template, whoName, otherName) {
  return String(template ?? '')
    .replaceAll('{{WHO}}', whoName)
    .replaceAll('{{OTHER}}', otherName);
}
