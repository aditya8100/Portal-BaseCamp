// Weather shaping for Open-Meteo responses (zero deps). Importable; no side effects.
export const WMO = new Map([
  [0, 'Clear'], [1, 'Mainly clear'], [2, 'Partly cloudy'], [3, 'Overcast'],
  [45, 'Fog'], [48, 'Icy fog'], [51, 'Light drizzle'], [53, 'Drizzle'], [55, 'Heavy drizzle'],
  [56, 'Freezing drizzle'], [57, 'Freezing drizzle'], [61, 'Light rain'], [63, 'Rain'], [65, 'Heavy rain'],
  [66, 'Freezing rain'], [67, 'Freezing rain'], [71, 'Light snow'], [73, 'Snow'], [75, 'Heavy snow'],
  [77, 'Snow grains'], [80, 'Light showers'], [81, 'Showers'], [82, 'Violent showers'],
  [85, 'Snow showers'], [86, 'Snow showers'], [95, 'Thunderstorm'], [96, 'Storm + hail'], [99, 'Storm + hail'],
]);

const label = (code) => WMO.get(code) || '';

// Daily sunrise/sunset arrive as local ISO "YYYY-MM-DDTHH:MM"; the app only
// needs today's wall-clock times to drive auto day/night.
const hhmm = (s) => (/^\d{4}-\d{2}-\d{2}T(\d{2}:\d{2})$/.exec(s || '') || [])[1] ?? null;

// Hourly times arrive as local ISO "YYYY-MM-DDTHH:MM" in the requested tz.
function hourlyToMs(s, utcOffsetSec) {
  const m = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})$/.exec(s || '');
  if (!m) return NaN;
  const [, Y, Mo, D, h, mi] = m.map(Number);
  return Date.UTC(Y, Mo - 1, D, h, mi) - utcOffsetSec * 1000;
}

// j: raw Open-Meteo JSON. Returns {current, days[8], hours[24], tz}.
export function shapeWeather(j, nowMs, tz, utcOffsetSec) {
  const d = j.daily || {};
  const h = j.hourly || {};
  const days = (d.time || []).map((date, i) => ({
    date,
    label: label(d.weather_code?.[i]),
    hi: Math.round(d.temperature_2m_max?.[i]),
    lo: Math.round(d.temperature_2m_min?.[i]),
    precip: d.precipitation_probability_max?.[i] ?? null,
  }));
  const hours = [];
  const times = h.time || [];
  for (let i = 0; i < times.length && hours.length < 24; i++) {
    const t = hourlyToMs(times[i], utcOffsetSec);
    if (!Number.isFinite(t) || t < nowMs - 30 * 60 * 1000) continue;
    hours.push({
      t,
      temp: Math.round(h.temperature_2m?.[i]),
      precip: h.precipitation_probability?.[i] ?? 0,
      label: label(h.weather_code?.[i]),
    });
  }
  return {
    tz,
    sun: { rise: hhmm(d.sunrise?.[0]), set: hhmm(d.sunset?.[0]) },
    current: {
      temp: Math.round(j.current?.temperature_2m),
      label: label(j.current?.weather_code),
    },
    days,
    hours,
  };
}
