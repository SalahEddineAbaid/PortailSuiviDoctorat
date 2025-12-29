import { Component, OnInit, OnDestroy, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { Observable, Subject, combineLatest } from 'rxjs';
import { takeUntil, map, startWith } from 'rxjs/operators';
import { FormControl } from '@angular/forms';

import { NotificationService } from '../../../core/services/notification.service';
import { NotificationResponse, NotificationType } from '../../../core/models/notification.model';

interface FilterOptions {
  type: NotificationType | 'ALL';
  status: 'ALL' | 'READ' | 'UNREAD';
  search: string;
}

@Component({
  selector: 'app-notification-list',
  templateUrl: './notification-list.component.html',
  styleUrls: ['./notification-list.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class NotificationListComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();

  // Données
  notifications$: Observable<NotificationResponse[]>;
  filteredNotifications$!: Observable<NotificationResponse[]>;
  unreadCount$: Observable<number>;
  loading = false;

  // Filtres
  searchControl = new FormControl('');
  typeFilter = new FormControl('ALL');
  statusFilter = new FormControl('ALL');

  // Options
  readonly notificationTypes = [
    { value: 'ALL', label: 'Tous les types' },
    { value: NotificationType.INFO, label: 'Information' },
    { value: NotificationType.SUCCESS, label: 'Succès' },
    { value: NotificationType.WARNING, label: 'Avertissement' },
    { value: NotificationType.ERROR, label: 'Erreur' },
    { value: NotificationType.REMINDER, label: 'Rappel' }
  ];

  readonly statusOptions = [
    { value: 'ALL', label: 'Toutes' },
    { value: 'UNREAD', label: 'Non lues' },
    { value: 'READ', label: 'Lues' }
  ];

  readonly NotificationType = NotificationType;

  constructor(
    private notificationService: NotificationService,
    private cdr: ChangeDetectorRef
  ) {
    this.notifications$ = this.notificationService.notifications$;
    this.unreadCount$ = this.notificationService.unreadCount$;
  }

  ngOnInit(): void {
    this.setupFilters();
    this.loadNotifications();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * 🔹 Configurer les filtres
   */
  private setupFilters(): void {
    const filters$ = combineLatest([
      this.searchControl.valueChanges.pipe(startWith('')),
      this.typeFilter.valueChanges.pipe(startWith('ALL')),
      this.statusFilter.valueChanges.pipe(startWith('ALL'))
    ]).pipe(
      map(([search, type, status]) => ({
        search: search || '',
        type: type as NotificationType | 'ALL',
        status: status as 'ALL' | 'READ' | 'UNREAD'
      }))
    );

    this.filteredNotifications$ = combineLatest([
      this.notifications$,
      filters$
    ]).pipe(
      map(([notifications, filters]) => this.applyFilters(notifications, filters))
    );
  }

  /**
   * 🔹 Appliquer les filtres
   */
  private applyFilters(notifications: NotificationResponse[], filters: FilterOptions): NotificationResponse[] {
    return notifications.filter(notification => {
      // Filtre par recherche
      if (filters.search) {
        const searchTerm = filters.search.toLowerCase();
        const matchesSearch = 
          notification.titre.toLowerCase().includes(searchTerm) ||
          notification.message.toLowerCase().includes(searchTerm);
        if (!matchesSearch) return false;
      }

      // Filtre par type
      if (filters.type !== 'ALL' && notification.type !== filters.type) {
        return false;
      }

      // Filtre par statut
      if (filters.status === 'READ' && !notification.lue) {
        return false;
      }
      if (filters.status === 'UNREAD' && notification.lue) {
        return false;
      }

      return true;
    });
  }

  /**
   * 🔹 Charger les notifications
   */
  loadNotifications(): void {
    this.loading = true;
    this.cdr.detectChanges();

    this.notificationService.refreshNotifications();
    
    // Simuler un délai de chargement
    setTimeout(() => {
      this.loading = false;
      this.cdr.detectChanges();
    }, 500);
  }

  /**
   * 🔹 Marquer toutes les notifications comme lues
   */
  markAllAsRead(): void {
    this.notificationService.markAllNotificationsAsRead();
  }

  /**
   * 🔹 Supprimer toutes les notifications lues
   */
  deleteAllRead(): void {
    const currentNotifications = this.notificationService.getCurrentNotifications();
    const readNotifications = currentNotifications.filter(n => n.lue);
    
    if (readNotifications.length === 0) {
      this.notificationService.showInfo('Aucune notification lue à supprimer');
      return;
    }

    if (confirm(`Êtes-vous sûr de vouloir supprimer ${readNotifications.length} notification(s) lue(s) ?`)) {
      readNotifications.forEach(notification => {
        this.notificationService.removeNotification(notification.id);
      });
      this.notificationService.showSuccess(`${readNotifications.length} notification(s) supprimée(s)`);
    }
  }

  /**
   * 🔹 Réinitialiser les filtres
   */
  resetFilters(): void {
    this.searchControl.setValue('');
    this.typeFilter.setValue('ALL');
    this.statusFilter.setValue('ALL');
  }

  /**
   * 🔹 Gérer le clic sur une notification
   */
  onNotificationClicked(notification: NotificationResponse): void {
    console.log('Notification cliquée:', notification);
    // Ici on pourrait naviguer vers une page spécifique selon le type de notification
  }

  /**
   * 🔹 Gérer la lecture d'une notification
   */
  onNotificationRead(notificationId: number): void {
    console.log('Notification marquée comme lue:', notificationId);
  }

  /**
   * 🔹 Gérer la suppression d'une notification
   */
  onNotificationDeleted(notificationId: number): void {
    console.log('Notification supprimée:', notificationId);
  }

  /**
   * 🔹 Obtenir le texte du filtre actuel
   */
  getFilterSummary(): string {
    const search = this.searchControl.value;
    const type = this.typeFilter.value;
    const status = this.statusFilter.value;

    const filters = [];
    
    if (search) {
      filters.push(`recherche: "${search}"`);
    }
    
    if (type !== 'ALL') {
      const typeLabel = this.notificationTypes.find(t => t.value === type)?.label;
      filters.push(`type: ${typeLabel}`);
    }
    
    if (status !== 'ALL') {
      const statusLabel = this.statusOptions.find(s => s.value === status)?.label;
      filters.push(`statut: ${statusLabel}`);
    }

    return filters.length > 0 ? `Filtres actifs: ${filters.join(', ')}` : '';
  }

  /**
   * 🔹 Vérifier si des filtres sont actifs
   */
  hasActiveFilters(): boolean {
    return !!this.searchControl.value || 
           this.typeFilter.value !== 'ALL' || 
           this.statusFilter.value !== 'ALL';
  }

  /**
   * 🔹 TrackBy function pour optimiser le rendu de la liste
   */
  trackByNotificationId(index: number, notification: NotificationResponse): number {
    return notification.id;
  }
}