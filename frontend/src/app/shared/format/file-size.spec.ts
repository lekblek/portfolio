import { formatFileSize } from './file-size';

const NBSP = '\u00a0';

describe('formatFileSize', () => {
  it('counts small files in bytes', () => {
    expect(formatFileSize(0)).toBe(`0${NBSP}octet`);
    expect(formatFileSize(1)).toBe(`1${NBSP}octet`);
    expect(formatFileSize(512)).toBe(`512${NBSP}octets`);
  });

  it('uses kilobytes then megabytes, with a french decimal comma', () => {
    expect(formatFileSize(1024)).toBe(`1${NBSP}Ko`);
    expect(formatFileSize(12_800)).toBe(`12,5${NBSP}Ko`);
    expect(formatFileSize(1_258_291)).toBe(`1,2${NBSP}Mo`);
  });

  it('rounds to one decimal at most', () => {
    expect(formatFileSize(10 * 1024 * 1024 - 1)).toBe(`10${NBSP}Mo`);
  });
});
