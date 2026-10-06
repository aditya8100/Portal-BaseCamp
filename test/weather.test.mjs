// Focused tests for weather shaping. Run: node --test test/weather.test.mjs
import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { shapeWeather } from '../weather.mjs';

// Fixed "now": Mon Oct 5 2026 17:10 CDT == 22:10Z. Chicago offset: -18000s.
const NOW = Date.UTC(2026, 9, 5, 22, 10, 0);
const OFF = -18000;

function fixture() {
  const times = [], temps = [], precips = [], codes = [];
  // 48 hourly points starting Oct 5 00:00 local.
  for (let i = 0; i < 48; i++) {
    const day = 5 + Math.floor(i / 24);
    const hh = String(i % 24).padStart(2, '0');
    times.push(`2026-10-${String(day).padStart(2, '0')}T${hh}:00`);
    temps.push(20 + (i % 12));
    precips.push(i % 5 === 0 ? 40 : 5);
    codes.push(i % 5 === 0 ? 61 : 1);
  }
  return {
    current: { temperature_2m: 30.4, weather_code: 1 },
    hourly: { time: times, temperature_2m: temps, precipitation_probability: precips, weather_code: codes },
    daily: {
      time: ['2026-10-05', '2026-10-06', '2026-10-07'],
      weather_code: [1, 3, 95],
      temperature_2m_max: [31.2, 29.9, 28.1],
      temperature_2m_min: [19.4, 18.2, 17.0],
      precipitation_probability_max: [5, 20, 80],
      sunrise: ['2026-10-05T07:23', '2026-10-06T07:24', '2026-10-07T07:24'],
      sunset: ['2026-10-05T19:12', '2026-10-06T19:11', '2026-10-07T19:10'],
    },
  };
}

describe('shapeWeather', () => {
  it('rounds current conditions and labels codes', () => {
    const w = shapeWeather(fixture(), NOW, 'America/Chicago', OFF);
    assert.equal(w.current.temp, 30);
    assert.equal(w.current.label, 'Mainly clear');
    assert.equal(w.days[2].label, 'Thunderstorm');
    assert.deepEqual([w.days[0].hi, w.days[0].lo, w.days[0].precip], [31, 19, 5]);
  });

  it('slices the next 24 hours from now', () => {
    const w = shapeWeather(fixture(), NOW, 'America/Chicago', OFF);
    assert.equal(w.hours.length, 24);
    // First hour: Oct 5 17:00 CDT == 22:00Z (within the 30-min grace of 22:10Z).
    assert.equal(new Date(w.hours[0].t).toISOString(), '2026-10-05T22:00:00.000Z');
    assert.ok(w.hours.every((h, i, a) => i === 0 || h.t > a[i - 1].t));
  });

  it('shapes today’s sunrise/sunset as HH:MM for auto theme', () => {
    const w = shapeWeather(fixture(), NOW, 'America/Chicago', OFF);
    assert.deepEqual(w.sun, { rise: '07:23', set: '19:12' });
  });

  it('nulls sun times when the feed omits them', () => {
    const f = fixture();
    delete f.daily.sunrise;
    delete f.daily.sunset;
    const w = shapeWeather(f, NOW, 'America/Chicago', OFF);
    assert.deepEqual(w.sun, { rise: null, set: null });
  });

  it('skips malformed hourly rows', () => {
    const f = fixture();
    f.hourly.time[17] = 'garbage';
    const w = shapeWeather(f, NOW, 'America/Chicago', OFF);
    assert.equal(w.hours.length, 24); // still fills 24 from later rows
    assert.ok(!w.hours.some((h) => Number.isNaN(h.t)));
  });
});
