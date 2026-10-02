import { Routes } from '@angular/router';

import { unsavedChangesGuard } from '../../../shared/forms/unsaved-changes.guard';
import { definite, newLabel, VOCABULARIES, VOCABULARY_KEYS } from './vocabularies';

/**
 * Taxonomie (`/admin/taxonomy`) : une entrée de la navigation, trois sous-sections (catégories,
 * tags, technologies), chacune avec sa liste et son formulaire de création ou de modification.
 * Le vocabulaire arrive aux pages par la donnée de route `vocabularyKey`.
 */
export const taxonomyRoutes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'categories' },
  ...VOCABULARY_KEYS.flatMap((key): Routes => {
    const vocabulary = VOCABULARIES[key];
    const data = { vocabularyKey: key, noindex: true };
    const form = () => import('./pages/term-form-page').then((m) => m.TermFormPage);
    return [
      {
        path: key,
        title: vocabulary.plural,
        data,
        loadComponent: () => import('./pages/term-list-page').then((m) => m.TermListPage),
      },
      {
        path: `${key}/new`,
        title: newLabel(vocabulary),
        data,
        canDeactivate: [unsavedChangesGuard],
        loadComponent: form,
      },
      {
        path: `${key}/:id`,
        title: `Modifier ${definite(vocabulary)}`,
        data,
        canDeactivate: [unsavedChangesGuard],
        loadComponent: form,
      },
    ];
  }),
];
