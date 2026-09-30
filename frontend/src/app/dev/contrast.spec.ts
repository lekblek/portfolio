import { contrastRatio, textLevel } from './contrast';

describe('contrastRatio', () => {
  it('gives 21 for black on white', () => {
    expect(contrastRatio('#000000', '#ffffff')).toBeCloseTo(21, 5);
  });

  it('gives 1 for identical colours', () => {
    expect(contrastRatio('#1e4f9a', '#1e4f9a')).toBe(1);
  });

  it('does not depend on the order of the colours', () => {
    expect(contrastRatio('#ffffff', '#1a2233')).toBeCloseTo(contrastRatio('#1a2233', '#ffffff'));
  });

  it('reads short hexadecimal colours', () => {
    expect(contrastRatio('#fff', '#000')).toBeCloseTo(21, 5);
  });

  it('matches the ratio documented for the accent colour', () => {
    expect(contrastRatio('#1e4f9a', '#ffffff')).toBeCloseTo(7.95, 2);
  });

  it('is not a number when a colour cannot be read', () => {
    expect(contrastRatio('var(--x)', '#ffffff')).toBeNaN();
  });
});

describe('textLevel', () => {
  it('names the level reached by normal text', () => {
    expect(textLevel(7.95)).toBe('AAA');
    expect(textLevel(5.69)).toBe('AA');
    expect(textLevel(3.2)).toBe('insuffisant');
  });
});
