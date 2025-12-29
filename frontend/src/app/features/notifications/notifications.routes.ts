import { Routes } from '@angular/router';
import { NotificationListComponent } from './notification-list/notification-list.component';
import { NotificationSettingsComponent } from './notification-settings/notification-settings.component';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'list',
    pathMatch: 'full'
  },
  {
    path: 'list',
    component: NotificationListComponent
  },
  {
    path: 'settings',
    component: NotificationSettingsComponent
  }
];

import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class NotificationsRoutingModule { }