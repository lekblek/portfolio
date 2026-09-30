import { inject, Service } from '@angular/core';
import { ActivatedRouteSnapshot, RouterStateSnapshot, TitleStrategy } from '@angular/router';

import { Seo } from './seo';

/**
 * À chaque navigation, remplace les métadonnées par celles de la route : titre
 * (« Titre — Blek Ngossanga », ou le titre de référence du site), description par défaut,
 * lien canonique, `noindex` si la route le demande (`data: { noindex: true }`).
 * Une page qui charge son contenu précise ensuite les siennes par `Seo.set`.
 */
@Service({ autoProvided: false })
export class PageTitleStrategy extends TitleStrategy {
  private readonly seo = inject(Seo);

  override updateTitle(snapshot: RouterStateSnapshot): void {
    this.seo.set({
      title: this.buildTitle(snapshot) ?? null,
      path: snapshot.url.split(/[?#]/)[0],
      noindex: deepest(snapshot.root).data['noindex'] === true,
    });
  }
}

function deepest(route: ActivatedRouteSnapshot): ActivatedRouteSnapshot {
  return route.firstChild ? deepest(route.firstChild) : route;
}
