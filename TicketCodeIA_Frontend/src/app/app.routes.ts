import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'dashboard',
    pathMatch: 'full'
  },
  {
    path: 'dashboard',
    loadComponent: () => import('./pages/dashboard/dashboard.component')
      .then(m => m.DashboardComponent)
  },
  {
    path: 'board',
    loadComponent: () => import('./pages/board/board.component')
      .then(m => m.BoardComponent)
  },
  {
    path: 'requirements',
    loadComponent: () => import('./pages/requirements/requirements.component')
      .then(m => m.RequirementsComponent)
  },
  {
    path: 'escalations',
    loadComponent: () => import('./pages/escalations/escalations.component')
      .then(m => m.EscalationsComponent)
  },
  {
    path: '**',
    redirectTo: 'dashboard'
  }
];
