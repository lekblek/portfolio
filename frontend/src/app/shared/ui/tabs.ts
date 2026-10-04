import { Tab, TabContent, TabList, TabPanel, Tabs } from '@angular/aria/tabs';

/**
 * Onglets (F31, 02-design-system §18) : motif *tabs* de l'APG par Angular Aria — `role` des
 * éléments, `aria-selected`, `aria-controls`, `aria-labelledby`, focus itinérant et flèches
 * gauche / droite, Début / Fin. Sélection explicite (`selectionMode="explicit"`) : une flèche
 * déplace le focus, Entrée ou Espace ouvre l'onglet.
 *
 * Apparence dans `styles/controls.css` (`.tab-list`, `.tab`, `.tab-panel`). Le contenu d'un
 * panneau est un `ng-template ngTabContent`, rendu à la première ouverture ; avec
 * `[preserveContent]="true"`, il reste ensuite dans le document (aucune saisie perdue au
 * changement d'onglet). Un panneau masqué reçoit `inert`, que la feuille masque.
 *
 * ```html
 * <div ngTabs>
 *   <div ngTabList class="tab-list" selectionMode="explicit" [(selectedTab)]="tab">
 *     <button ngTab class="tab" value="visuel">Visuel</button>
 *   </div>
 *   <div ngTabPanel class="tab-panel" value="visuel" [preserveContent]="true">
 *     <ng-template ngTabContent>…</ng-template>
 *   </div>
 * </div>
 * ```
 */
export const TABS = [Tabs, TabList, Tab, TabPanel, TabContent] as const;
