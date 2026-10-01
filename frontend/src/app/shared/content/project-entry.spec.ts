import { Component, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { ProjectSummary } from '../../core/api/api-types';
import { ProjectEntry } from './project-entry';

const PROJECT: ProjectSummary = {
  title: 'Détection de changements',
  slug: 'detection-de-changements',
  shortDescription: 'Traitement d’images satellitaires.',
  stage: 'COMPLETED',
  startDate: '2023-02-01',
  endDate: '2023-11-30',
  featured: false,
  cover: { url: '/api/public/media/cover', width: 1200, height: 750, altText: 'Carte des zones' },
  technologies: [
    { name: 'PyTorch', slug: 'pytorch' },
    { name: 'Spring Boot', slug: 'spring-boot' },
  ],
};

@Component({
  imports: [ProjectEntry],
  template: `<app-project-entry
    [project]="project()"
    [headingLevel]="level()"
    currentTechnology="pytorch"
  />`,
})
class Host {
  readonly project = signal<ProjectSummary>(PROJECT);
  readonly level = signal<2 | 3>(2);
}

describe('ProjectEntry', () => {
  function render(project: ProjectSummary = PROJECT, level: 2 | 3 = 2) {
    TestBed.configureTestingModule({ providers: [provideRouter([])] });
    const fixture = TestBed.createComponent(Host);
    fixture.componentInstance.project.set(project);
    fixture.componentInstance.level.set(level);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  it('links the title to the project and states its stage and period', () => {
    const element = render();

    const title = element.querySelector('h2 a');
    expect(title?.textContent).toBe('Détection de changements');
    expect(title?.getAttribute('href')).toBe('/projects/detection-de-changements');
    expect(
      element
        .querySelector('.entry-meta')
        ?.textContent?.replace(/[ \n]+/g, ' ')
        .trim(),
    ).toBe('Terminé, février 2023\u00a0– novembre 2023');
  });

  it('links each technology to the filtered list and marks the current filter', () => {
    const element = render();

    const links = Array.from(element.querySelectorAll('app-term-links a'));
    expect(links.map((link) => link.getAttribute('href'))).toEqual([
      '/projects?technology=pytorch',
      '/projects?technology=spring-boot',
    ]);
    expect(links[0].getAttribute('aria-current')).toBe('true');
    expect(links[1].hasAttribute('aria-current')).toBe(false);
  });

  it('shows the cover with its alternative text', () => {
    expect(render().querySelector('img')?.getAttribute('alt')).toBe('Carte des zones');
  });

  it('keeps a coherent entry without cover, end date or technology', () => {
    const element = render(
      { ...PROJECT, cover: null, endDate: null, stage: 'IN_PROGRESS', technologies: [] },
      3,
    );

    expect(element.querySelector('h3 a')).not.toBeNull();
    expect(element.querySelector('img, app-term-links, .entry-cover')).toBeNull();
    expect(
      element
        .querySelector('.entry-meta')
        ?.textContent?.replace(/[ \n]+/g, ' ')
        .trim(),
    ).toBe('En cours, depuis février 2023');
  });
});
