import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { NotificationComponent } from './notification/notification.component';
import { NotificationListComponent } from './notification-list/notification-list.component';
import { NotificationBellComponent } from './notification-bell/notification-bell.component';
import { NotificationDropdownComponent } from './notification-dropdown/notification-dropdown.component';
import { NotificationSettingsComponent } from './notification-settings/notification-settings.component';
import { NotificationsRoutingModule } from './notifications.routes';

@NgModule({
  declarations: [
    NotificationComponent,
    NotificationListComponent,
    NotificationBellComponent,
    NotificationDropdownComponent,
    NotificationSettingsComponent
  ],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule,
    SharedModule,
    NotificationsRoutingModule
  ],
  exports: [
    NotificationBellComponent,
    NotificationDropdownComponent
  ]
})
export class NotificationsModule { }