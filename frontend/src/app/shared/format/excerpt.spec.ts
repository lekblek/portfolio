import { markdownExcerpt } from './excerpt';

describe('markdownExcerpt', () => {
  it('keeps the first paragraph as plain text', () => {
    expect(
      markdownExcerpt(
        '## Titre\n\nUne **série** sur [Spring](https://x.example)[^1].\n\nSuite.',
        200,
      ),
    ).toBe('Une série sur Spring.');
  });

  it('skips code and formulas before the first paragraph', () => {
    expect(markdownExcerpt('```java\nclass A {}\n```\n\n$$\nx\n$$\n\nTexte.', 200)).toBe('Texte.');
  });

  it('cuts a long paragraph on a word boundary', () => {
    expect(markdownExcerpt('Un deux trois quatre cinq', 12)).toBe('Un deux…');
  });
});
