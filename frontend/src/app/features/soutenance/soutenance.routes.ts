import { Routes } from '@angular/router';
import { SoutenanceContainer } from './soutenance-container/soutenance-container';
import { SoutenanceDashboard } from './soutenance-dashboard/soutenance-dashboard';
import { SoutenanceForm } from './soutenance-form';
import { SoutenanceDetail } from './soutenance-detail';
import { SoutenanceList } from './soutenance-list';
import { authGuard } from '../../core/guards/auth.guard';
import { roleGuard } from '../../core/guards/role.guard';
import { RoleName } from '../../core/models/role.model';

export const soutenanceRoutes: Routes = [
  {
    path: '',
    component: SoutenanceContainer,
    canActivate: [authGuard, roleGuard],
    data: { role: RoleName.DOCTORANT },
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      { 
        path: 'dashboard', 
        component: SoutenanceDashboard,
        title: 'Mes Soutenances'
      },
      { 
        path: 'nouvelle', 
        component: SoutenanceForm,
        title: 'Nouvelle Demande de Soutenance'
      },
      { 
        path: 'liste', 
        component: SoutenanceList,
        title: 'Liste des Soutenances'
      },
      { 
        path: ':id', 
        component: SoutenanceDetail,
        title: 'Détails Soutenance'
      },
      { 
        path: ':id/edit', 
        component: SoutenanceForm,
        title: 'Modifier Demande de Soutenance'
      }
    ]
  }
];