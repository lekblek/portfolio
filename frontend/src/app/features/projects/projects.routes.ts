import { Routes } from '@angular/router';

export const projectsRoutes: Routes = [
  {
    path: '',
    title: 'Projets',
    loadComponent: () => import('./pages/project-list').then((m) => m.ProjectList),
  },
  {
    // Titre remplacé par celui du projet dès sa lecture
    path: ':slug',
    title: 'Projet',
    loadComponent: () => import('./pages/project-detail').then((m) => m.ProjectDetail),
  },
];
