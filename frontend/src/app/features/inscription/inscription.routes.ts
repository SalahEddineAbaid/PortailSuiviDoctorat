import { Routes } from '@angular/router';
import { InscriptionContainer } from './inscription-container/inscription-container';
import { InscriptionDashboard } from './inscription-dashboard/inscription-dashboard';
import { InscriptionForm } from './inscription-form/inscription-form';
import { ReinscriptionForm } from './reinscription-form/reinscription-form';
import { InscriptionDetail } from './inscription-detail/inscription-detail';
import { authGuard } from '../../core/guards/auth.guard';
import { roleGuard } from '../../core/guards/role.guard';
import { RoleName } from '../../core/models/role.model';

export const inscriptionRoutes: Routes = [
  {
    path: '',
    component: InscriptionContainer,
    canActivate: [authGuard, roleGuard],
    data: { role: RoleName.DOCTORANT },
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      { 
        path: 'dashboard', 
        component: InscriptionDashboard,
        title: 'Mes Inscriptions'
      },
      { 
        path: 'nouvelle', 
        component: InscriptionForm,
        title: 'Nouvelle Inscription'
      },
      { 
        path: 'reinscription', 
        component: ReinscriptionForm,
        title: 'Réinscription'
      },
      { 
        path: ':id', 
        component: InscriptionDetail,
        title: 'Détails Inscription'
      },
      { 
        path: ':id/edit', 
        component: InscriptionForm,
        title: 'Modifier Inscription'
      }
    ]
  }
];