// Focused regression tests for the calendar parser. Run: node --test test/
import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { parseIcs, upcoming, weekView, zonedToUtc } from '../cal.mjs';

const TZ = 'America/Chicago';
// Fixed "now": Mon Oct 5 2026 16:00 CDT == 21:00Z.
const NOW = Date.UTC(2026, 9, 5, 21, 0, 0);

const FIXTURE = `BEGIN:VCALENDAR
VERSION:2.0
BEGIN:VEVENT
UID:single-1
DTEND;TZID=America/Chicago:20261008T213000
DTSTART;TZID=America/Chicago:20261008T194500
SUMMARY:ACL Taping
LOCATION:Moody Theater
END:VEVENT
BEGIN:VEVENT
UID:utc-1
DTSTART:20261009T130000Z
DTEND:20261009T140000Z
SUMMARY:UTC Lunch
END:VEVENT
BEGIN:VEVENT
UID:allday-1
DTSTART;VALUE=DATE:20261010
SUMMARY:Day Off
END:VEVENT
BEGIN:VEVENT
UID:weekly-1
DTSTART;TZID=America/Chicago:20260930T190000
RRULE:FREQ=WEEKLY;COUNT=4
SUMMARY:Wednesday League
END:VEVENT
BEGIN:VEVENT
UID:exdate-1
DTSTART;TZID=America/Chicago:20261006T090000
RRULE:FREQ=DAILY;COUNT=5
EXDATE;TZID=America/Chicago:20261007T090000
SUMMARY:Standup
END:VEVENT
END:VCALENDAR`;

const events = () => parseIcs(FIXTURE, TZ);
const occ = () => upcoming(events(), NOW - 6 * 3600 * 1000, NOW + 14 * 24 * 3600 * 1000, NOW, TZ);

describe('parseIcs', () => {
  it('parses all events', () => {
    assert.equal(events().length, 5);
  });

  it('keeps TZID datetimes in the right month (no double month shift)', () => {
    const ev = events().find((e) => e.title === 'ACL Taping');
    // Oct 8 19:45 CDT == Oct 9 00:45Z.
    assert.equal(new Date(ev.start).toISOString(), '2026-10-09T00:45:00.000Z');
    assert.equal(ev.location, 'Moody Theater');
    assert.equal(ev.dur, 6300000); // 19:45 -> 21:30; DTEND order no longer matters
  });

  it('parses UTC and all-day forms', () => {
    const utc = events().find((e) => e.title === 'UTC Lunch');
    assert.equal(new Date(utc.start).toISOString(), '2026-10-09T13:00:00.000Z');
    const ad = events().find((e) => e.title === 'Day Off');
    assert.equal(ad.allDay, true);
    assert.equal(new Date(ad.start).toISOString(), '2026-10-10T05:00:00.000Z'); // midnight CDT
  });

  it('zonedToUtc matches a known CDT wall time', () => {
    assert.equal(new Date(zonedToUtc(2026, 10, 8, 19, 45, 0, TZ)).toISOString(), '2026-10-09T00:45:00.000Z');
  });
});

describe('upcoming', () => {
  it('includes single future events and sorts by start', () => {
    const titles = occ().map((o) => o.title);
    assert.ok(titles.includes('ACL Taping'));
    assert.ok(titles.includes('UTC Lunch'));
    const starts = occ().map((o) => o.start);
    assert.deepEqual(starts, [...starts].sort((a, b) => a - b));
  });

  it('expands weekly COUNT recurrences within the window', () => {
    const got = occ().filter((o) => o.title === 'Wednesday League')
      .map((o) => new Date(o.start).toISOString());
    // Series: Sep 30, Oct 7, 14, 21 (Wednesdays 19:00 CDT); window keeps Oct 7 + 14.
    assert.deepEqual(got, ['2026-10-08T00:00:00.000Z', '2026-10-15T00:00:00.000Z']);
  });

  it('honors EXDATE', () => {
    const got = occ().filter((o) => o.title === 'Standup')
      .map((o) => new Date(o.start).toISOString().slice(0, 10));
    // Oct 6..10 daily minus Oct 7.
    assert.deepEqual(got, ['2026-10-06', '2026-10-08', '2026-10-09', '2026-10-10']);
  });
});

describe('weekView', () => {
  // NOW is Mon Oct 5 2026 -> week Sun Oct 4 .. Sat Oct 10.
  const week = () => weekView(events(), NOW, TZ);

  it('builds a Sunday-start week containing now', () => {
    const days = week().days;
    assert.equal(days.length, 7);
    assert.equal(days[0].key, '2026-10-04');
    assert.equal(days[0].label, 'Sun');
    assert.equal(days[6].key, '2026-10-10');
    assert.equal(days[6].label, 'Sat');
  });

  it('places timed events with wall-clock minutes', () => {
    const thu = week().days[4];
    const acl = thu.timed.find((e) => e.title === 'ACL Taping');
    assert.ok(acl);
    assert.equal(acl.startMin, 19 * 60 + 45);
    assert.equal(acl.endMin, 21 * 60 + 30);
  });

  it('collects all-day events separately', () => {
    const sat = week().days[6];
    assert.ok(sat.allDay.some((e) => e.title === 'Day Off'));
    assert.ok(!sat.timed.some((e) => e.title === 'Day Off'));
  });

  it('expands recurrences into the right days and honors EXDATE', () => {
    const days = week().days;
    assert.ok(days[3].timed.some((e) => e.title === 'Wednesday League')); // Wed Oct 7
    assert.ok(days[2].timed.some((e) => e.title === 'Standup')); // Tue Oct 6
    assert.ok(!days[3].timed.some((e) => e.title === 'Standup')); // Wed excluded
  });
});
