import { Routes } from '@angular/router';

import { SearchPage } from './pages/search-page';

export const searchRoutes: Routes = [
  { path: '', title: 'Recherche', data: { noindex: true }, component: SearchPage },
];
