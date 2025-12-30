import { Routes } from '@angular/router';
import { NotificationListComponent } from './notification-list/notification-list.component';
import { NotificationSettingsComponent } from './notification-settings/notification-settings.component';

export const notificationsRoutes: Routes = [
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