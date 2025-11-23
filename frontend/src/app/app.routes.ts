import { Routes } from '@angular/router';
import { Login } from './features/auth/login/login';
import { Register } from './features/auth/register/register';
import { DashboardContainer } from './features/dashboard/dashboard-container/dashboard-container';
import { DoctorantDashboard } from './features/dashboard/doctorant-dashboard/doctorant-dashboard';
import { DirecteurDashboard } from './features/dashboard/directeur-dashboard/directeur-dashboard';
import { AdminDashboard } from './features/dashboard/admin-dashboard/admin-dashboard';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';
import { RoleName } from './core/models/role.model';

export const routes: Routes = [
  // Redirection par défaut
  { path: '', redirectTo: '/login', pathMatch: 'full' },

  // Routes publiques
  { path: 'login', component: Login },
  { path: 'register', component: Register },

  // Routes protégées (dashboard)
  {
    path: 'dashboard',
    component: DashboardContainer,
    canActivate: [authGuard],  // ✅ Vérification authentification
    children: [
      { path: '', redirectTo: 'doctorant', pathMatch: 'full' },
      {
        path: 'doctorant',
        component: DoctorantDashboard,
        canActivate: [roleGuard],  // ✅ Vérification rôle
        data: { role: RoleName.DOCTORANT }
      },
      {
        path: 'directeur',
        component: DirecteurDashboard,
        canActivate: [roleGuard],
        data: { role: RoleName.DIRECTEUR }
      },
      {
        path: 'admin',
        component: AdminDashboard,
        canActivate: [roleGuard],
        data: { role: RoleName.ADMIN }
      }
    ]
  },

  // Route 404
  { path: '**', redirectTo: '/login' }
];