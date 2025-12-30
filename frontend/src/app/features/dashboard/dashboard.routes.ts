import { Routes } from '@angular/router';
import { DashboardContainer } from './dashboard-container/dashboard-container';
import { DoctorantDashboard } from './doctorant-dashboard/doctorant-dashboard';
import { DirecteurDashboardComponent } from './directeur-dashboard/directeur-dashboard.component';
import { AdminDashboard } from './admin-dashboard/admin-dashboard';
import { roleGuard } from '../../core/guards/role.guard';
import { RoleName } from '../../core/models/role.model';

export const dashboardRoutes: Routes = [
  {
    path: '',
    component: DashboardContainer,
    children: [
      { path: '', redirectTo: 'doctorant', pathMatch: 'full' },
      {
        path: 'doctorant',
        component: DoctorantDashboard,
        canActivate: [roleGuard],
        data: { role: RoleName.DOCTORANT }
      },
      {
        path: 'directeur',
        component: DirecteurDashboardComponent,
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
  }
];