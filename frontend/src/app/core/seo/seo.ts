import { DOCUMENT, inject, Service } from '@angular/core';
import { Meta, Title } from '@angular/platform-browser';

import { serializeJsonLd } from './json-ld';
import { SITE_DESCRIPTION, SITE_NAME, SITE_TITLE, SITE_URL } from './site-config';

export interface SeoData {
  /** Titre propre de la page ; absent : titre de référence du site. */
  title?: string | null;
  /** Absente : description par défaut du site. */
  description?: string | null;
  /**
   * Chemin canonique (`/projects/portfolio`), sans ancre ; avec la requête seulement quand elle
   * désigne une autre page de contenu (`/projects?page=2`).
   */
  path: string;
  type?: 'website' | 'article' | 'profile';
  /** Adresse de l'image de partage, relative au site ou absolue. */
  image?: string | null;
  /** Page à ne pas indexer (introuvable, résultats de recherche). */
  noindex?: boolean;
  /** Données structurées propres à la page (`ProfilePage`, `Article`…) ; absentes : retirées. */
  jsonLd?: object | null;
}

const WEBSITE_JSON_LD_ID = 'json-ld-site';
const PAGE_JSON_LD_ID = 'json-ld-page';

/**
 * Métadonnées de la page courante : titre, description, lien canonique, Open Graph, robots.
 * Chaque appel remplace entièrement les valeurs de la page précédente. La stratégie de titre
 * l'appelle à chaque navigation avec les valeurs de la route ; une page qui charge son contenu
 * l'appelle de nouveau avec les siennes.
 */
@Service()
export class Seo {
  private readonly title = inject(Title);
  private readonly meta = inject(Meta);
  private readonly document = inject(DOCUMENT);
  private readonly siteUrl = inject(SITE_URL);

  set(data: SeoData): void {
    const title = data.title ? `${data.title} — ${SITE_NAME}` : SITE_TITLE;
    const description = data.description ?? SITE_DESCRIPTION;
    const url = this.absolute(data.path);

    this.title.setTitle(title);
    this.meta.updateTag({ name: 'description', content: description });
    this.canonical().setAttribute('href', url);

    this.meta.updateTag({ property: 'og:title', content: title });
    this.meta.updateTag({ property: 'og:description', content: description });
    this.meta.updateTag({ property: 'og:type', content: data.type ?? 'website' });
    this.meta.updateTag({ property: 'og:url', content: url });
    this.meta.updateTag({ property: 'og:site_name', content: SITE_NAME });
    this.meta.updateTag({ property: 'og:locale', content: 'fr_FR' });
    if (data.image) {
      this.meta.updateTag({ property: 'og:image', content: this.absolute(data.image) });
      this.meta.updateTag({ name: 'twitter:card', content: 'summary_large_image' });
    } else {
      this.meta.removeTag("property='og:image'");
      this.meta.updateTag({ name: 'twitter:card', content: 'summary' });
    }

    if (data.noindex) {
      this.meta.updateTag({ name: 'robots', content: 'noindex' });
    } else {
      this.meta.removeTag("name='robots'");
    }

    if (data.jsonLd) {
      this.jsonLd(PAGE_JSON_LD_ID, data.jsonLd);
    } else {
      this.document.getElementById(PAGE_JSON_LD_ID)?.remove();
    }
  }

  /** Données structurées `WebSite`, posées une fois par le shell public. */
  setWebsiteJsonLd(): void {
    this.jsonLd(WEBSITE_JSON_LD_ID, {
      '@context': 'https://schema.org',
      '@type': 'WebSite',
      name: SITE_NAME,
      url: this.absolute('/'),
      inLanguage: 'fr',
      description: SITE_DESCRIPTION,
    });
  }

  /** Adresse absolue d'un chemin du site (lien canonique, image, données structurées). */
  absolute(path: string): string {
    return new URL(path, this.siteUrl).href;
  }

  private canonical(): HTMLLinkElement {
    const existing = this.document.head.querySelector<HTMLLinkElement>('link[rel="canonical"]');
    if (existing) {
      return existing;
    }
    const link = this.document.createElement('link');
    link.setAttribute('rel', 'canonical');
    this.document.head.appendChild(link);
    return link;
  }

  private jsonLd(id: string, data: object): void {
    let script = this.document.getElementById(id);
    if (script === null) {
      script = this.document.createElement('script');
      script.id = id;
      script.setAttribute('type', 'application/ld+json');
      this.document.head.appendChild(script);
    }
    script.textContent = serializeJsonLd(data);
  }
}
