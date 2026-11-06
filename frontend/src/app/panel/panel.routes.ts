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
    path: 'agenda',
    title: 'Agenda · Reservas',
    loadComponent: () => import('./agenda/agenda').then((m) => m.Agenda),
  },
  {
    path: 'empleados',
    title: 'Empleados · Reservas',
    canActivate: [propietarioGuard],
    loadComponent: () => import('./empleados/empleados').then((m) => m.Empleados),
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
    path: 'excepciones',
    title: 'Vacaciones y festivos · Reservas',
    canActivate: [propietarioGuard],
    loadComponent: () => import('./excepciones/excepciones').then((m) => m.Excepciones),
  },
  {
    path: 'recursos/:id/horario',
    title: 'Horario · Reservas',
    canActivate: [propietarioGuard],
    loadComponent: () => import('./horario/horario-recurso').then((m) => m.HorarioRecurso),
  },
];
