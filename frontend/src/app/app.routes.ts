import { Routes } from '@angular/router';

import { authGuard, invitadoGuard } from './core/auth/auth.guards';

/** Cada página se carga bajo demanda (lazy loading): el bundle inicial solo trae lo común. */
export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'panel' },
  {
    path: 'login',
    title: 'Iniciar sesión · Reservas',
    canActivate: [invitadoGuard],
    loadComponent: () => import('./auth/login/login').then((m) => m.Login),
  },
  {
    path: 'registro',
    title: 'Alta de negocio · Reservas',
    canActivate: [invitadoGuard],
    loadComponent: () => import('./auth/registro/registro').then((m) => m.Registro),
  },
  {
    path: 'panel',
    title: 'Panel · Reservas',
    canActivate: [authGuard],
    loadComponent: () => import('./panel/panel').then((m) => m.Panel),
  },
  // Se sustituirá por la página pública /:slug
  { path: '**', redirectTo: '' },
];
