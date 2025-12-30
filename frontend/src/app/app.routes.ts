import { Routes } from '@angular/router';
import { Login } from './features/auth/login/login';
import { Register } from './features/auth/register/register';
import { ProfileComponent } from './features/auth/profile/profile.component';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';
import { RoleName } from './core/models/role.model';

export const routes: Routes = [
  // Redirection par défaut
  { path: '', redirectTo: '/login', pathMatch: 'full' },

  // Routes publiques
  { path: 'login', component: Login },
  { path: 'register', component: Register },

  // Routes protégées (profil)
  { 
    path: 'profile', 
    component: ProfileComponent,
    canActivate: [authGuard]
  },

  // Routes protégées (dashboard) - Lazy loading
  {
    path: 'dashboard',
    loadChildren: () => import('./features/dashboard/dashboard.routes').then(m => m.dashboardRoutes),
    canActivate: [authGuard]
  },

  // Routes protégées (inscription) - Lazy loading
  {
    path: 'inscription',
    loadChildren: () => import('./features/inscription/inscription.routes').then(m => m.inscriptionRoutes),
    canActivate: [authGuard, roleGuard],
    data: { roles: [RoleName.DOCTORANT] }
  },

  // Routes protégées (soutenance) - Lazy loading
  {
    path: 'soutenance',
    loadChildren: () => import('./features/soutenance/soutenance.routes').then(m => m.soutenanceRoutes),
    canActivate: [authGuard, roleGuard],
    data: { roles: [RoleName.DOCTORANT] }
  },

  // Routes protégées (administration) - Lazy loading
  {
    path: 'admin',
    loadChildren: () => import('./features/admin/admin.routes').then(m => m.adminRoutes),
    canActivate: [authGuard, roleGuard],
    data: { roles: [RoleName.ADMIN] }
  },

  // Routes protégées (notifications) - Lazy loading
  {
    path: 'notifications',
    loadChildren: () => import('./features/notifications/notifications.routes').then(m => m.notificationsRoutes),
    canActivate: [authGuard]
  },

  // Route 404
  { path: '**', redirectTo: '/login' }
];