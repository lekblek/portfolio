import { inject, Service } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { RouterStateSnapshot, TitleStrategy } from '@angular/router';

/** Nom provisoire du site, remplacé par la configuration du site (étape SEO). */
export const SITE_NAME = 'Portfolio';

/** « Titre de la page — Nom du site », ou le nom du site seul pour une route sans titre. */
@Service({ autoProvided: false })
export class PageTitleStrategy extends TitleStrategy {
  private readonly title = inject(Title);

  override updateTitle(snapshot: RouterStateSnapshot): void {
    const pageTitle = this.buildTitle(snapshot);
    this.title.setTitle(pageTitle ? `${pageTitle} — ${SITE_NAME}` : SITE_NAME);
  }
}
