import { Component, OnInit, OnDestroy, ChangeDetectionStrategy, Output, EventEmitter } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

import { NotificationService } from '../../../core/services/notification.service';
import { NotificationResponse } from '../../../core/models/notification.model';

@Component({
  selector: 'app-notification-bell',
  templateUrl: './notification-bell.component.html',
  styleUrls: ['./notification-bell.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class NotificationBellComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();

  @Output() bellClicked = new EventEmitter<void>();
  @Output() notificationClicked = new EventEmitter<NotificationResponse>();

  unreadCount$: Observable<number>;
  notifications$: Observable<NotificationResponse[]>;
  
  isDropdownOpen = false;
  hasNewNotifications = false;

  constructor(private notificationService: NotificationService) {
    this.unreadCount$ = this.notificationService.unreadCount$;
    this.notifications$ = this.notificationService.notifications$;
  }

  ngOnInit(): void {
    // Écouter les nouvelles notifications pour l'animation
    this.notificationService.websocketMessages$
      .pipe(takeUntil(this.destroy$))
      .subscribe(message => {
        if (message.type === 'NOTIFICATION') {
          this.triggerNewNotificationAnimation();
        }
      });

    // Demander la permission pour les notifications système
    this.notificationService.requestNotificationPermission();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * 🔹 Basculer l'affichage du dropdown
   */
  toggleDropdown(): void {
    this.isDropdownOpen = !this.isDropdownOpen;
    this.bellClicked.emit();
    
    // Réinitialiser l'animation de nouvelle notification
    if (this.isDropdownOpen) {
      this.hasNewNotifications = false;
    }
  }

  /**
   * 🔹 Fermer le dropdown
   */
  closeDropdown(): void {
    this.isDropdownOpen = false;
  }

  /**
   * 🔹 Gérer le clic sur une notification
   */
  onNotificationClick(notification: NotificationResponse): void {
    this.notificationClicked.emit(notification);
    this.closeDropdown();
  }

  /**
   * 🔹 Déclencher l'animation de nouvelle notification
   */
  private triggerNewNotificationAnimation(): void {
    this.hasNewNotifications = true;
    
    // Réinitialiser après l'animation
    setTimeout(() => {
      if (!this.isDropdownOpen) {
        this.hasNewNotifications = false;
      }
    }, 2000);
  }

  /**
   * 🔹 Gérer les clics en dehors du composant
   */
  onDocumentClick(event: Event): void {
    const target = event.target as HTMLElement;
    const bellElement = document.querySelector('.notification-bell');
    
    if (bellElement && !bellElement.contains(target)) {
      this.closeDropdown();
    }
  }

  /**
   * 🔹 Naviguer vers la page complète des notifications
   */
  viewAllNotifications(): void {
    this.closeDropdown();
    // Navigation sera gérée par le composant parent
  }

  /**
   * 🔹 Marquer toutes les notifications comme lues
   */
  markAllAsRead(): void {
    this.notificationService.markAllNotificationsAsRead();
  }

  /**
   * 🔹 TrackBy function pour optimiser le rendu de la liste
   */
  trackByNotificationId(index: number, notification: NotificationResponse): number {
    return notification.id;
  }
}