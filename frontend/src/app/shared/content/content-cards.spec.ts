import { Component, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { ProjectSummary, PublicationSummary } from '../../core/api/api-types';
import { plateMark } from './content-plate';
import { NewsItem } from './news-item';
import { ProjectCard } from './project-card';
import { PublicationCard } from './publication-card';

const PROJECT: ProjectSummary = {
  title: 'RoadSense, détection routière',
  slug: 'roadsense',
  shortDescription: 'Détection des piétons.',
  stage: 'IN_PROGRESS',
  startDate: '2023-01-01',
  endDate: null,
  featured: true,
  cover: null,
  technologies: ['Docker', 'Python', 'PyTorch', 'OpenCV', 'Kafka', 'Grafana'].map((name) => ({
    name,
    slug: name.toLowerCase(),
  })),
};

const ARTICLE: PublicationSummary = {
  type: 'ARTICLE',
  title: 'Signal Forms en pratique',
  slug: 'signal-forms',
  summary: 'Listes ordonnables.',
  publishedAt: '2026-09-15T08:00:00Z',
  featured: false,
  readingTimeMinutes: 4,
  cover: { url: '/api/public/media/a.webp', width: 1500, height: 1000, altText: 'Écran' },
  category: { name: 'Frontend', slug: 'frontend' },
  tags: [{ name: 'Angular', slug: 'angular' }],
};

@Component({
  imports: [ProjectCard, PublicationCard, NewsItem],
  template: `
    <app-project-card [project]="project" [currentTechnology]="technology()" />
    <app-publication-card [publication]="article" [lead]="lead()" />
    <app-news-item [item]="news" />
  `,
})
class Host {
  readonly project = PROJECT;
  readonly article = ARTICLE;
  readonly news: PublicationSummary = { ...ARTICLE, type: 'NEWS', cover: null, slug: 'lancement' };
  readonly technology = signal<string | null>(null);
  readonly lead = signal(false);
}

function render() {
  TestBed.configureTestingModule({ providers: [provideRouter([])] });
  const fixture = TestBed.createComponent(Host);
  fixture.detectChanges();
  return { fixture, element: fixture.nativeElement as HTMLElement };
}

describe('plateMark', () => {
  it('keeps the initials of the first two significant words', () => {
    expect(plateMark('Portfolio full-stack')).toBe('PF');
    expect(plateMark('La vision par ordinateur appliquée')).toBe('VP');
    expect(plateMark('Spring Boot de zéro à la production')).toBe('SB');
    expect(plateMark('Œuvre')).toBe('Œ');
  });
});

describe('content cards', () => {
  it('shows a project card: title first, four technologies at most, an empty plate hidden from readers', () => {
    const { element } = render();
    const card = element.querySelector('app-project-card')!;

    expect(card.querySelector('article')?.firstElementChild?.tagName).toBe('H2');
    expect(card.querySelector('h2 a')?.getAttribute('href')).toBe('/projects/roadsense');
    expect(Array.from(card.querySelectorAll('.term-link')).map((link) => link.textContent)).toEqual(
      ['Docker', 'Python', 'PyTorch', 'OpenCV'],
    );
    expect(card.textContent).toContain('2 autres technologies');
    const plate = card.querySelector('.plate-empty')!;
    expect(plate.getAttribute('aria-hidden')).toBe('true');
    expect(plate.textContent?.trim()).toBe('RD');
  });

  it('keeps the technology of the filter among the four shown', () => {
    const { fixture, element } = render();
    fixture.componentInstance.technology.set('grafana');
    fixture.detectChanges();

    const names = Array.from(element.querySelectorAll('app-project-card .term-link')).map(
      (link) => link.textContent,
    );
    expect(names).toEqual(['Docker', 'Python', 'PyTorch', 'Grafana']);
    expect(element.querySelector('app-project-card [aria-current]')?.textContent).toBe('Grafana');
  });

  it('shows an article card with its category, date and reading time, and tags only in the lead', () => {
    const { fixture, element } = render();
    const card = () => element.querySelector('app-publication-card')!;

    expect(card().querySelector('.card-category')?.getAttribute('href')).toBe(
      '/articles?category=frontend',
    );
    expect(card().textContent).toContain('15 septembre 2026');
    expect(card().textContent).toContain('4\u00a0min de lecture');
    expect(card().querySelector('img')?.getAttribute('alt')).toBe('Écran');
    expect(card().querySelector('.term-list')).toBeNull();

    fixture.componentInstance.lead.set(true);
    fixture.detectChanges();
    expect(card().querySelector('.card-lead')).not.toBeNull();
    expect(card().querySelector('.term-list')?.textContent).toContain('Angular');
    expect(card().querySelector('.plate-wide')).not.toBeNull();
  });

  it('shows a news item as a dated dispatch, the full date read by assistive technologies', () => {
    const { element } = render();
    const item = element.querySelector('app-news-item')!;

    expect(item.querySelector('h3 a')?.getAttribute('href')).toBe('/news/lancement');
    expect(item.querySelector('time')?.getAttribute('datetime')).toBe('2026-09-15');
    expect(item.querySelector('time .sr-only')?.textContent).toBe('15 septembre 2026');
    expect(item.querySelector('.news-day')?.getAttribute('aria-hidden')).toBe('true');
    expect(item.textContent).not.toContain('min de lecture');
  });
});
