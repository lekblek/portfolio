import { agree, capitalized, definite, newLabel, quoted, VOCABULARIES } from './vocabularies';

describe('vocabularies', () => {
  it('speaks of each vocabulary with its own gender', () => {
    const { categories, tags, technologies } = VOCABULARIES;

    expect([definite(categories), definite(tags), definite(technologies)]).toEqual([
      'la catégorie',
      'le tag',
      'la technologie',
    ]);
    expect([newLabel(categories), newLabel(tags)]).toEqual(['Nouvelle catégorie', 'Nouveau tag']);
    expect([agree(categories, 'créé'), agree(tags, 'créé')]).toEqual(['créée', 'créé']);
    expect(capitalized('tag')).toBe('Tag');
  });

  it('quotes a name the french way, with non-breaking spaces', () => {
    expect(quoted('Java')).toBe('«\u00a0Java\u00a0»');
  });

  it('keeps the bounds of each vocabulary', () => {
    expect(VOCABULARIES.tags.nameMax).toBe(60);
    expect(VOCABULARIES.categories.description).toBe(true);
    expect(VOCABULARIES.technologies.displayOrder).toBe(true);
    expect(VOCABULARIES.technologies.usedBy).toBe('un projet');
  });
});
