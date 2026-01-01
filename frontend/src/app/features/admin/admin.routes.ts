import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { authGuard } from '../../core/guards/auth.guard';
import { roleGuard } from '../../core/guards/role.guard';
import { RoleName } from '../../core/models/role.model';

import { AdminContainerComponent } from './admin-container/admin-container.component';
import { CampagneManagementComponent } from './campagne-management/campagne-management.component';
import { UserManagementComponent } from './user-management/user-management.component';
import { DossierValidationComponent } from './dossier-validation/dossier-validation.component';
import { ParametrageComponent } from './parametrage/parametrage.component';
import { UserList } from './users/user-list/user-list';
import { UserDetailComponent } from './users/user-detail/user-detail.component';
import { UserEditComponent } from './users/user-edit/user-edit.component';

const routes: Routes = [
  {
    path: '',
    component: AdminContainerComponent,
    canActivate: [authGuard, roleGuard],
    data: { role: RoleName.ADMIN },
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'dashboard', redirectTo: '/dashboard/admin', pathMatch: 'full' },
      { path: 'campagnes', component: CampagneManagementComponent },
      { path: 'users', component: UserList },
      { path: 'users/:id', component: UserDetailComponent },
      { path: 'users/:id/edit', component: UserEditComponent },
      { path: 'utilisateurs', redirectTo: 'users', pathMatch: 'full' },
      { path: 'validations', component: DossierValidationComponent },
      { path: 'dossiers', component: DossierValidationComponent },
      { path: 'parametrage', component: ParametrageComponent },
      { path: 'configuration', redirectTo: 'parametrage', pathMatch: 'full' },
      { path: 'statistics', redirectTo: '/dashboard/admin', pathMatch: 'full' }
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class AdminRoutingModule { }

export { routes as adminRoutes };