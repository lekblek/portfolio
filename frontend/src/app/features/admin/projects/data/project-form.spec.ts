import { Injector, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { form } from '@angular/forms/signals';

import { AdminProject } from '../../../../core/api/api-types';
import { EMPTY_PROJECT, ProjectModel, projectSchema, toModel, toRequest } from './project-form';

const SAVED: AdminProject = {
  id: 7,
  title: 'Atlas ERP',
  slug: 'atlas-erp',
  slugLocked: true,
  shortDescription: 'ERP pour PME.',
  descriptionMarkdown: '## Contexte',
  stage: 'COMPLETED',
  visibility: 'PUBLISHED',
  startDate: '2024-03-01',
  endDate: '2025-06-30',
  repositoryUrl: 'https://example.test/git',
  demoUrl: null,
  featured: true,
  displayOrder: 2,
  technologyIds: [1, 3],
  coverMediaId: 12,
  screenshots: [
    { mediaId: 13, caption: 'Tableau de bord' },
    { mediaId: 14, caption: null },
  ],
};

function projectForm(model: ProjectModel) {
  return form(signal(model), projectSchema, { injector: TestBed.inject(Injector) });
}

function valid(): ProjectModel {
  return { ...EMPTY_PROJECT, title: 'Atlas', shortDescription: 'Résumé', startDate: '2024-03-01' };
}

describe('project form', () => {
  it('turns a saved project into a model and back, keeping the slug unless it changed', () => {
    const model = toModel(SAVED);

    expect(model.demoUrl).toBe('');
    expect(model.displayOrder).toBe('2');
    expect(model.screenshots[1].caption).toBe('');
    expect(toRequest(model, SAVED.slug)).toEqual({
      title: 'Atlas ERP',
      slug: undefined,
      shortDescription: 'ERP pour PME.',
      descriptionMarkdown: '## Contexte',
      stage: 'COMPLETED',
      visibility: 'PUBLISHED',
      startDate: '2024-03-01',
      endDate: '2025-06-30',
      repositoryUrl: 'https://example.test/git',
      demoUrl: undefined,
      featured: true,
      displayOrder: 2,
      technologyIds: [1, 3],
      coverMediaId: 12,
      screenshots: [
        { mediaId: 13, caption: 'Tableau de bord' },
        { mediaId: 14, caption: undefined },
      ],
    });
    expect(toRequest({ ...model, slug: ' atlas ' }, SAVED.slug).slug).toBe('atlas');
  });

  it('sends no end date for a project in progress, and 0 for an empty order', () => {
    const request = toRequest({ ...valid(), endDate: '2025-01-01', displayOrder: ' ' }, '');

    expect(request.endDate).toBeUndefined();
    expect(request.displayOrder).toBe(0);
    expect(request.slug).toBeUndefined();
  });

  it('accepts a complete project', () => {
    expect(projectForm(valid())().valid()).toBe(true);
  });

  it('asks a completed project for its end date, never before its start', () => {
    const missing = projectForm({ ...valid(), stage: 'COMPLETED' });
    expect(missing.endDate().errors()[0]?.message).toBe(
      'Indiquez la date de fin d’un projet terminé.',
    );

    const before = projectForm({ ...valid(), stage: 'COMPLETED', endDate: '2024-01-01' });
    expect(before.endDate().errors()[0]?.message).toBe('La fin précède le début.');
  });

  it('checks the title, the slug, the addresses and the order', () => {
    const tree = projectForm({
      ...valid(),
      title: '!!!',
      slug: 'Atlas ERP',
      repositoryUrl: 'example.test',
      displayOrder: '-1',
    });

    expect(tree.title().errors()[0]?.message).toContain('au moins une lettre ou un chiffre');
    expect(tree.slug().valid()).toBe(false);
    expect(tree.repositoryUrl().errors()[0]?.message).toContain('https://');
    expect(tree.displayOrder().valid()).toBe(false);
  });
});
