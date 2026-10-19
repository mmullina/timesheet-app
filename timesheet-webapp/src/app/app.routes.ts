import { Routes } from '@angular/router';
import { signedIn, adminOnly } from './auth';



export const routes: Routes = [
  { path: 'login', loadComponent: () => import('./login').then((m) => m.Login) },
  { path: 'week', canActivate: [signedIn], loadComponent: () => import('./week').then((m) => m.Week) },
  { path: 'admin', canActivate: [signedIn, adminOnly], loadComponent: () => import('./admin').then((m) => m.Admin) },
  { path: '', pathMatch: 'full', redirectTo: 'week' },
  { path: '**', redirectTo: 'week' },
  { path: 'admin', canActivate: [signedIn, adminOnly], loadComponent: () => import('./admin').then((m) => m.Admin) }
];