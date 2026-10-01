import { Series } from '../../../core/api/api-types';
import { markdownExcerpt } from '../../../shared/format/excerpt';

/**
 * Données structurées d'une série : `CreativeWorkSeries` dont les parties sont ses articles
 * visibles, dans l'ordre de lecture (`position`). `absolute` rend absolue une adresse du site.
 */
export function seriesJsonLd(series: Series, absolute: (path: string) => string): object {
  const work: Record<string, unknown> = {
    '@context': 'https://schema.org',
    '@type': 'CreativeWorkSeries',
    name: series.title,
    url: absolute(`/series/${series.slug}`),
    inLanguage: 'fr',
    hasPart: series.chapters.map((chapter) => ({
      '@type': 'Article',
      headline: chapter.title,
      url: absolute(`/articles/${chapter.slug}`),
      position: chapter.position,
      datePublished: chapter.publishedAt,
    })),
  };
  const description = markdownExcerpt(series.descriptionMarkdown, 300);
  if (description) {
    work['description'] = description;
  }
  if (series.cover) {
    work['image'] = absolute(series.cover.url);
  }
  return work;
}
