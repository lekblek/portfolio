import { Publication } from '../../../core/api/api-types';
import { SITE_NAME } from '../../../core/seo/site-config';
import { publicationPath } from '../../../shared/content/publication-entry';

/**
 * Données structurées d'une publication : `Article` (article technique) ou `NewsArticle`
 * (actualité), avec les seules données publiées ; auteur : la personne du site.
 * `absolute` rend absolue une adresse du site.
 */
export function publicationJsonLd(
  publication: Publication,
  absolute: (path: string) => string,
): object {
  const article: Record<string, unknown> = {
    '@context': 'https://schema.org',
    '@type': publication.type === 'NEWS' ? 'NewsArticle' : 'Article',
    headline: publication.seoTitle ?? publication.title,
    description: publication.seoDescription ?? publication.summary,
    url: absolute(publicationPath(publication.type, publication.slug)),
    mainEntityOfPage: absolute(publicationPath(publication.type, publication.slug)),
    inLanguage: 'fr',
    datePublished: publication.publishedAt,
    author: { '@type': 'Person', name: SITE_NAME, url: absolute('/about') },
  };
  if (publication.cover) {
    article['image'] = absolute(publication.cover.url);
  }
  if (publication.category) {
    article['articleSection'] = publication.category.name;
  }
  if (publication.tags.length > 0) {
    article['keywords'] = publication.tags.map((tag) => tag.name).join(', ');
  }
  return article;
}
