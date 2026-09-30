import { serializeJsonLd } from './json-ld';

describe('serializeJsonLd', () => {
  it('keeps the data readable as JSON', () => {
    const data = { '@type': 'WebSite', name: 'Blek Ngossanga', url: 'https://example.org' };

    expect(JSON.parse(serializeJsonLd(data))).toEqual(data);
  });

  it('cannot close the script element that contains it', () => {
    const serialized = serializeJsonLd({ headline: '</script><script>alert(1)</script>' });

    expect(serialized).not.toContain('<');
    expect(serialized).not.toContain('>');
    expect(JSON.parse(serialized).headline).toBe('</script><script>alert(1)</script>');
  });

  it('escapes ampersands and javascript line terminators', () => {
    const serialized = serializeJsonLd({ text: 'a & b\u2028c\u2029d' });

    expect(serialized).not.toMatch(/[&\u2028\u2029]/);
    expect(JSON.parse(serialized).text).toBe('a & b\u2028c\u2029d');
  });
});
