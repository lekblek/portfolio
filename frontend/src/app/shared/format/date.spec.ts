import {
  formatDay,
  formatDayTime,
  formatMonth,
  formatPeriod,
  instantFromSiteTime,
  isoDay,
  siteTimeInput,
} from './date';

describe('formatMonth', () => {
  it('writes the month and the year in french', () => {
    expect(formatMonth('2024-01-15')).toBe('janvier 2024');
    expect(formatMonth('2023-08-01')).toBe('août 2023');
  });

  it('never shifts the day with the time zone, at either end of a month', () => {
    expect(formatMonth('2024-03-01')).toBe('mars 2024');
    expect(formatMonth('2024-12-31')).toBe('décembre 2024');
  });
});

describe('formatPeriod', () => {
  it('joins both ends with a dash that never starts a line', () => {
    expect(formatPeriod('2018-09-01', '2023-06-30')).toBe('septembre 2018\u00a0– juin 2023');
  });

  it('says since when for a period in progress', () => {
    expect(formatPeriod('2024-01-01', null)).toBe('depuis janvier 2024');
  });

  it('writes a single month when the period starts and ends in it', () => {
    expect(formatPeriod('2024-03-01', '2024-03-28')).toBe('mars 2024');
  });
});

describe('formatDay', () => {
  it('writes the day of the instant in the reference time zone', () => {
    expect(formatDay('2026-09-17T19:30:00Z')).toBe('17 septembre 2026');
    // 23 h 30 UTC le 30 septembre : déjà le 1er octobre à Paris
    expect(formatDay('2026-09-30T23:30:00Z')).toBe('1er octobre 2026');
    expect(isoDay('2026-09-30T23:30:00Z')).toBe('2026-10-01');
  });
});

describe('formatDayTime', () => {
  it('writes the day and the time of the instant in the reference time zone', () => {
    expect(formatDayTime('2026-10-02T08:42:00Z')).toBe('2 octobre 2026 à 10:42');
    expect(formatDayTime('2026-09-30T22:05:00Z')).toBe('1er octobre 2026 à 00:05');
  });
});

describe('site time inputs', () => {
  it('reads a wall-clock time in Paris, summer and winter', () => {
    expect(instantFromSiteTime('2026-10-15T09:30')).toBe('2026-10-15T07:30:00.000Z');
    expect(instantFromSiteTime('2026-12-15T09:30')).toBe('2026-12-15T08:30:00.000Z');
  });

  it('writes an instant back as a Paris wall-clock time', () => {
    expect(siteTimeInput('2026-10-15T07:30:00Z')).toBe('2026-10-15T09:30');
    expect(siteTimeInput('2026-12-15T08:30:00Z')).toBe('2026-12-15T09:30');
  });
});
