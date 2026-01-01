import { Component, Input, Output, EventEmitter, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import { NotificationService } from '../../../core/services/notification.service';
import { NotificationResponse, NotificationType } from '../../../core/models/notification.model';

@Component({
  selector: 'app-notification-dropdown',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './notification-dropdown.component.html',
  styleUrls: ['./notification-dropdown.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class NotificationDropdownComponent {
  @Input() maxNotifications: number = 5;
  @Input() showHeader: boolean = true;
  @Input() showFooter: boolean = true;
  @Input() showActions: boolean = true;

  @Output() notificationClicked = new EventEmitter<NotificationResponse>();
  @Output() viewAllClicked = new EventEmitter<void>();
  @Output() markAllReadClicked = new EventEmitter<void>();
  @Output() closeClicked = new EventEmitter<void>();

  notifications$: Observable<NotificationResponse[]>;
  unreadCount$: Observable<number>;
  recentNotifications$: Observable<NotificationResponse[]>;
  hasMoreNotifications$: Observable<boolean>;

  readonly NotificationType = NotificationType;

  constructor(private notificationService: NotificationService) {
    this.notifications$ = this.notificationService.notifications$;
    this.unreadCount$ = this.notificationService.unreadCount$;
    
    // Notifications récentes limitées
    this.recentNotifications$ = this.notifications$.pipe(
      map(notifications => notifications.slice(0, this.maxNotifications))
    );

    // Vérifier s'il y a plus de notifications
    this.hasMoreNotifications$ = this.notifications$.pipe(
      map(notifications => notifications.length > this.maxNotifications)
    );
  }

  /**
   * 🔹 Gérer le clic sur une notification
   */
  onNotificationClick(notification: NotificationResponse): void {
    // Marquer comme lue si pas encore lue
    if (!notification.lue) {
      this.notificationService.markNotificationAsRead(notification.id);
    }
    
    this.notificationClicked.emit(notification);
  }

  /**
   * 🔹 Marquer toutes les notifications comme lues
   */
  onMarkAllAsRead(): void {
    this.notificationService.markAllNotificationsAsRead();
    this.markAllReadClicked.emit();
  }

  /**
   * 🔹 Voir toutes les notifications
   */
  onViewAll(): void {
    this.viewAllClicked.emit();
  }

  /**
   * 🔹 Fermer le dropdown
   */
  onClose(): void {
    this.closeClicked.emit();
  }

  /**
   * 🔹 TrackBy function pour optimiser le rendu
   */
  trackByNotificationId(index: number, notification: NotificationResponse): number {
    return notification.id;
  }

  /**
   * 🔹 Obtenir le texte du compteur de notifications supplémentaires
   */
  getMoreNotificationsText(totalCount: number): string {
    const moreCount = totalCount - this.maxNotifications;
    return moreCount === 1 ? '1 notification de plus' : `${moreCount} notifications de plus`;
  }

  /**
   * 🔹 Obtenir l'icône selon le type de notification
   */
  getNotificationIcon(type: NotificationType): string {
    return this.notificationService.getNotificationIcon(type);
  }

  /**
   * 🔹 Obtenir la couleur selon le type de notification
   */
  getNotificationColor(type: NotificationType): string {
    return this.notificationService.getNotificationColor(type);
  }

  /**
   * 🔹 Formater la date relative
   */
  getRelativeTime(date: Date): string {
    const now = new Date();
    const notificationDate = new Date(date);
    const diffInMinutes = Math.floor((now.getTime() - notificationDate.getTime()) / (1000 * 60));

    if (diffInMinutes < 1) {
      return 'À l\'instant';
    } else if (diffInMinutes < 60) {
      return `${diffInMinutes} min`;
    } else if (diffInMinutes < 1440) {
      const hours = Math.floor(diffInMinutes / 60);
      return `${hours}h`;
    } else {
      const days = Math.floor(diffInMinutes / 1440);
      return `${days}j`;
    }
  }

  /**
   * 🔹 Vérifier si la notification est récente (moins de 1h)
   */
  isRecentNotification(date: Date): boolean {
    const now = new Date();
    const notificationDate = new Date(date);
    const diffInMinutes = (now.getTime() - notificationDate.getTime()) / (1000 * 60);
    return diffInMinutes < 60;
  }
}