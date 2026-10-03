import { Injector, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { form } from '@angular/forms/signals';

import { AdminPublication } from '../../../../core/api/api-types';
import {
  EMPTY_PUBLICATION,
  PublicationModel,
  publicationSchema,
  toCreateRequest,
  toModel,
  toUpdateRequest,
} from './publication-form';
import { publicPath } from './publications';
import { transitionsFrom } from './transitions';

const SAVED: AdminPublication = {
  id: 4,
  type: 'ARTICLE',
  title: 'Monolithe modulaire',
  slug: 'monolithe-modulaire',
  slugLocked: true,
  summary: 'Neuf modules.',
  contentMarkdown: '## Architecture',
  status: 'PUBLISHED',
  publishedAt: '2026-06-16T08:00:00Z',
  featured: false,
  categoryId: 3,
  tagIds: [1, 2],
  coverMediaId: null,
  seoTitle: null,
  seoDescription: 'Description',
  createdAt: '2026-06-01T08:00:00Z',
  updatedAt: '2026-06-16T08:00:00Z',
};

describe('publication transitions', () => {
  it('offers only the transitions of the editorial cycle (D-AV)', () => {
    const targets = (status: AdminPublication['status']) =>
      transitionsFrom(status).map((transition) => transition.target);

    expect(targets('DRAFT')).toEqual(['PUBLISHED', 'SCHEDULED', 'IN_REVIEW']);
    expect(targets('IN_REVIEW')).toEqual(['PUBLISHED', 'SCHEDULED', 'DRAFT']);
    expect(targets('SCHEDULED')).toEqual(['PUBLISHED', 'SCHEDULED', 'DRAFT']);
    expect(targets('PUBLISHED')).toEqual(['ARCHIVED']);
    expect(targets('ARCHIVED')).toEqual(['PUBLISHED', 'DRAFT']);
    expect(transitionsFrom('PUBLISHED')[0].confirm).toBe(true);
    expect(transitionsFrom('DRAFT')[1].needsDate).toBe(true);
  });
});

describe('publication form', () => {
  it('turns a saved publication into a model and back, keeping the frozen slug', () => {
    const model = toModel(SAVED);

    expect(model.categoryId).toBe('3');
    expect(model.seoTitle).toBe('');
    expect(toUpdateRequest(model, SAVED.slug)).toEqual({
      title: 'Monolithe modulaire',
      slug: undefined,
      summary: 'Neuf modules.',
      contentMarkdown: '## Architecture',
      categoryId: 3,
      tagIds: [1, 2],
      coverMediaId: undefined,
      featured: false,
      seoTitle: undefined,
      seoDescription: 'Description',
    });
  });

  it('creates with the type, and without a category when none is chosen', () => {
    const request = toCreateRequest(
      { ...EMPTY_PUBLICATION, title: ' Note ', summary: 'Résumé' },
      'NEWS',
    );

    expect(request).toMatchObject({
      type: 'NEWS',
      title: 'Note',
      categoryId: undefined,
      tagIds: [],
    });
  });

  it('requires a title and a summary, and bounds the search texts', () => {
    const tree = form(
      signal<PublicationModel>({ ...EMPTY_PUBLICATION, summary: '   ', seoTitle: 'x'.repeat(121) }),
      publicationSchema,
      { injector: TestBed.inject(Injector) },
    );

    expect(tree.title().errors()[0]?.message).toBe('Indiquez le titre.');
    expect(tree.summary().errors()[0]?.message).toBe('Indiquez le résumé.');
    expect(tree.seoTitle().errors()[0]?.message).toBe('120 caractères au plus.');
  });

  it('builds the public address from the type', () => {
    expect(publicPath('ARTICLE', 'a')).toBe('/articles/a');
    expect(publicPath('NEWS', 'b')).toBe('/news/b');
  });
});
