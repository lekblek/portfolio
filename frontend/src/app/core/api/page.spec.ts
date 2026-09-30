import { toHttpParams } from './page';

describe('toHttpParams', () => {
  it('converts the first page of the url to the first page of the api', () => {
    expect(toHttpParams({ page: 1 }).get('page')).toBe('0');
  });

  it('converts later pages to the zero-based page of the api', () => {
    expect(toHttpParams({ page: 3 }).get('page')).toBe('2');
  });

  it('asks for the first page when the page is absent or invalid', () => {
    for (const page of [undefined, 0, -2, 1.5, Number.NaN]) {
      expect(toHttpParams({ page }).get('page')).toBe('0');
    }
  });

  it('passes filters that have a value', () => {
    const params = toHttpParams({ page: 2, technology: 'angular' });

    expect(params.get('technology')).toBe('angular');
  });

  it('leaves out absent or empty filters', () => {
    const params = toHttpParams({ technology: undefined, category: null, tag: '' });

    expect(params.keys()).toEqual(['page']);
  });
});
