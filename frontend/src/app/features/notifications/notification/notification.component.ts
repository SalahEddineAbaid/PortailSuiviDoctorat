import { Component, Input, Output, EventEmitter, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { NotificationResponse, NotificationType } from '../../../core/models/notification.model';
import { NotificationService } from '../../../core/services/notification.service';

@Component({
  selector: 'app-notification',
  standalone: true,
  imports: [CommonModule, MatIconModule, MatTooltipModule],
  templateUrl: './notification.component.html',
  styleUrls: ['./notification.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class NotificationComponent {
  @Input() notification!: NotificationResponse;
  @Input() showActions: boolean = true;
  @Input() compact: boolean = false;
  
  @Output() notificationRead = new EventEmitter<number>();
  @Output() notificationDeleted = new EventEmitter<number>();
  @Output() notificationClicked = new EventEmitter<NotificationResponse>();

  readonly NotificationType = NotificationType;

  constructor(private notificationService: NotificationService) {}

  /**
   * 🔹 Marquer la notification comme lue
   */
  markAsRead(): void {
    if (!this.notification.lue) {
      this.notificationService.markNotificationAsRead(this.notification.id);
      this.notificationRead.emit(this.notification.id);
    }
  }

  /**
   * 🔹 Supprimer la notification
   */
  deleteNotification(event: Event): void {
    event.stopPropagation();
    this.notificationService.removeNotification(this.notification.id);
    this.notificationDeleted.emit(this.notification.id);
  }

  /**
   * 🔹 Cliquer sur la notification
   */
  onNotificationClick(): void {
    this.markAsRead();
    this.notificationClicked.emit(this.notification);
  }

  /**
   * 🔹 Obtenir l'icône de la notification
   */
  getNotificationIcon(): string {
    return this.notificationService.getNotificationIcon(this.notification.type);
  }

  /**
   * 🔹 Obtenir la couleur de la notification
   */
  getNotificationColor(): string {
    return this.notificationService.getNotificationColor(this.notification.type);
  }

  /**
   * 🔹 Obtenir le temps relatif depuis la création
   */
  getRelativeTime(): string {
    const now = new Date();
    const created = new Date(this.notification.dateCreation);
    const diffInMinutes = Math.floor((now.getTime() - created.getTime()) / (1000 * 60));

    if (diffInMinutes < 1) {
      return 'À l\'instant';
    } else if (diffInMinutes < 60) {
      return `Il y a ${diffInMinutes} min`;
    } else if (diffInMinutes < 1440) {
      const hours = Math.floor(diffInMinutes / 60);
      return `Il y a ${hours}h`;
    } else {
      const days = Math.floor(diffInMinutes / 1440);
      return `Il y a ${days}j`;
    }
  }

  /**
   * 🔹 Vérifier si la notification est récente (moins de 24h)
   */
  isRecent(): boolean {
    const now = new Date();
    const created = new Date(this.notification.dateCreation);
    const diffInHours = (now.getTime() - created.getTime()) / (1000 * 60 * 60);
    return diffInHours < 24;
  }
}