import { Routes } from '@angular/router';

import { propietarioGuard } from '../core/auth/auth.guards';

/** Páginas hijas del panel; se muestran dentro del layout con el menú lateral. */
export const panelRoutes: Routes = [
  {
    path: '',
    title: 'Panel · Reservas',
    loadComponent: () => import('./inicio/inicio').then((m) => m.Inicio),
  },
  {
    path: 'servicios',
    title: 'Servicios · Reservas',
    canActivate: [propietarioGuard],
    loadComponent: () => import('./servicios/servicios').then((m) => m.Servicios),
  },
  {
    path: 'recursos',
    title: 'Recursos · Reservas',
    canActivate: [propietarioGuard],
    loadComponent: () => import('./recursos/recursos').then((m) => m.Recursos),
  },
  {
    path: 'recursos/:id/horario',
    title: 'Horario · Reservas',
    canActivate: [propietarioGuard],
    loadComponent: () => import('./horario/horario-recurso').then((m) => m.HorarioRecurso),
  },
];
