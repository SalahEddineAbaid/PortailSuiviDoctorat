import { Injectable, OnDestroy } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject, Subject } from 'rxjs';
import { takeUntil, filter } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import {
  type Notification,
  NotificationRequest,
  NotificationResponse,
  NotificationSettings,
  NotificationType,
  WebSocketMessage
} from '../models/notification.model';
import { WebSocketService, WebSocketState } from './websocket.service';

export interface UINotification {
  id: string;
  type: 'success' | 'error' | 'warning' | 'info';
  title: string;
  message: string;
  duration?: number;
  timestamp: Date;
}

@Injectable({
  providedIn: 'root'
})
export class NotificationService implements OnDestroy {
  private readonly API_URL = `${environment.apiUrl}/notifications`;
  private readonly WS_URL = environment.apiUrl.replace('http', 'ws').replace('/api', '/ws');
  private destroy$ = new Subject<void>();

  // Subjects pour la gestion des notifications en temps réel
  private notificationsSubject = new BehaviorSubject<NotificationResponse[]>([]);
  private unreadCountSubject = new BehaviorSubject<number>(0);

  // Subjects pour les notifications UI (toasts)
  private uiNotificationsSubject = new BehaviorSubject<UINotification[]>([]);
  public uiNotifications$ = this.uiNotificationsSubject.asObservable();

  // Observables publics
  public notifications$ = this.notificationsSubject.asObservable();
  public unreadCount$ = this.unreadCountSubject.asObservable();
  public websocketMessages$: Observable<any>;
  public websocketState$: Observable<WebSocketState>;

  // Flag pour savoir si le service de notification backend est disponible
  private notificationServiceAvailable = true;

  constructor(
    private http: HttpClient,
    private webSocketService: WebSocketService
  ) {
    // Initialiser les observables WebSocket après l'injection
    this.websocketMessages$ = this.webSocketService.messages$.pipe(
      filter(message => message.type === 'NOTIFICATION')
    );
    this.websocketState$ = this.webSocketService.state$;
    
    // ⚠️ Ne PAS charger automatiquement les notifications au démarrage
    // Les notifications seront chargées quand l'utilisateur sera authentifié
    // this.loadNotifications();
    // this.setupWebSocketSubscriptions();
    
    console.log('🔔 [NOTIFICATION SERVICE] Service initialisé (chargement différé)');
  }

  // ===== HTTP ENDPOINTS =====

  /**
   * 🔹 Récupérer mes notifications
   * Note: Peut retourner 404 si le service notification n'est pas déployé
   */
  getMyNotifications(): Observable<NotificationResponse[]> {
    // Ne pas logger ici - laisser le caller gérer les erreurs
    return this.http.get<NotificationResponse[]>(`${this.API_URL}/me`);
  }

  /**
   * 🔹 Marquer une notification comme lue
   */
  markAsRead(notificationId: number): Observable<any> {
    console.log('📤 [NOTIFICATION SERVICE] Marquer comme lue:', notificationId);
    return this.http.put(`${this.API_URL}/${notificationId}/read`, {});
  }

  /**
   * 🔹 Marquer toutes les notifications comme lues
   */
  markAllAsRead(): Observable<any> {
    console.log('📤 [NOTIFICATION SERVICE] Marquer toutes comme lues');
    return this.http.put(`${this.API_URL}/read-all`, {});
  }

  /**
   * 🔹 Supprimer une notification
   */
  deleteNotification(notificationId: number): Observable<any> {
    console.log('📤 [NOTIFICATION SERVICE] Suppression notification:', notificationId);
    return this.http.delete(`${this.API_URL}/${notificationId}`);
  }

  /**
   * 🔹 Récupérer les paramètres de notification
   */
  getNotificationSettings(): Observable<NotificationSettings> {
    console.log('📤 [NOTIFICATION SERVICE] Récupération paramètres');
    return this.http.get<NotificationSettings>(`${this.API_URL}/settings`);
  }

  /**
   * 🔹 Mettre à jour les paramètres de notification
   */
  updateNotificationSettings(settings: NotificationSettings): Observable<NotificationSettings> {
    console.log('📤 [NOTIFICATION SERVICE] Mise à jour paramètres:', settings);
    return this.http.put<NotificationSettings>(`${this.API_URL}/settings`, settings);
  }

  /**
   * 🔹 Envoyer une notification (ADMIN)
   */
  sendNotification(notification: NotificationRequest): Observable<NotificationResponse> {
    console.log('📤 [NOTIFICATION SERVICE] Envoi notification:', notification);
    return this.http.post<NotificationResponse>(this.API_URL, notification);
  }

  // ===== WEBSOCKET MANAGEMENT =====

  /**
   * 🔹 Configurer les abonnements WebSocket
   */
  private setupWebSocketSubscriptions(): void {
    // Écouter les messages WebSocket
    this.webSocketService.messages$
      .pipe(takeUntil(this.destroy$))
      .subscribe(message => {
        if (message.type === 'NOTIFICATION') {
          this.handleNewNotification(message.data);
        }
      });

    // Écouter les changements d'état WebSocket
    this.webSocketService.state$
      .pipe(takeUntil(this.destroy$))
      .subscribe(state => {
        console.log('🔌 [NOTIFICATION SERVICE] État WebSocket:', state);
        
        if (state === WebSocketState.ERROR) {
          this.showError('Connexion temps réel interrompue', 'Notifications');
        } else if (state === WebSocketState.CONNECTED) {
          this.showSuccess('Connexion temps réel établie', 'Notifications');
        }
      });
  }

  /**
   * 🔹 Se connecter au WebSocket pour les notifications temps réel
   */
  connectWebSocket(): void {
    const token = localStorage.getItem(environment.tokenKey);
    if (!token) {
      console.warn('⚠️ [NOTIFICATION SERVICE] Pas de token pour WebSocket');
      return;
    }

    console.log('🔌 [NOTIFICATION SERVICE] Connexion WebSocket notifications');
    
    this.webSocketService.connectWithAuth(this.WS_URL, token, {
      maxReconnectAttempts: 5,
      reconnectInterval: 3000,
      enableHeartbeat: true,
      heartbeatInterval: 30000
    }).pipe(takeUntil(this.destroy$))
    .subscribe();
  }

  /**
   * 🔹 Déconnecter le WebSocket
   */
  disconnectWebSocket(): void {
    console.log('🔌 [NOTIFICATION SERVICE] Déconnexion WebSocket');
    this.webSocketService.disconnect();
  }

  /**
   * 🔹 Obtenir l'état de la connexion WebSocket
   */
  getWebSocketState(): WebSocketState {
    return this.webSocketService.getState();
  }

  /**
   * 🔹 Vérifier si WebSocket est connecté
   */
  isWebSocketConnected(): boolean {
    return this.webSocketService.isConnected();
  }

  // ===== STATE MANAGEMENT =====

  /**
   * 🔹 Initialiser le service de notifications (à appeler après connexion)
   */
  public initialize(): void {
    if (!this.notificationServiceAvailable) {
      console.log('ℹ️ [NOTIFICATION SERVICE] Service non disponible, initialisation ignorée');
      return;
    }
    
    console.log('🔔 [NOTIFICATION SERVICE] Initialisation...');
    this.loadNotifications();
    this.setupWebSocketSubscriptions();
  }

  /**
   * 🔹 Charger les notifications depuis l'API
   */
  private loadNotifications(): void {
    this.getMyNotifications().subscribe({
      next: (notifications) => {
        console.log('✅ [NOTIFICATION SERVICE] Notifications chargées:', notifications.length);
        this.notificationsSubject.next(notifications);
        this.updateUnreadCount(notifications);
        this.notificationServiceAvailable = true;
      },
      error: (error) => {
        // Si 404, le service n'existe pas - silencieux
        if (error.status === 404) {
          // Service non disponible - pas de log d'erreur
          this.notificationServiceAvailable = false;
        } else if (error.status !== 401) {
          // Ne pas logger les erreurs 401 (utilisateur non connecté)
          console.warn('⚠️ [NOTIFICATION SERVICE] Service indisponible:', error.status);
        }
        // Initialiser avec un tableau vide en cas d'erreur
        this.notificationsSubject.next([]);
        this.unreadCountSubject.next(0);
      }
    });
  }

  /**
   * 🔹 Gérer une nouvelle notification reçue via WebSocket
   */
  private handleNewNotification(notification: NotificationResponse): void {
    const currentNotifications = this.notificationsSubject.value;
    const updatedNotifications = [notification, ...currentNotifications];
    
    this.notificationsSubject.next(updatedNotifications);
    this.updateUnreadCount(updatedNotifications);
    
    // Afficher une notification système si supporté
    this.showSystemNotification(notification);
  }

  /**
   * 🔹 Mettre à jour le compteur de notifications non lues
   */
  private updateUnreadCount(notifications: NotificationResponse[]): void {
    const unreadCount = notifications.filter(n => !n.lue).length;
    this.unreadCountSubject.next(unreadCount);
  }

  /**
   * 🔹 Afficher une notification système (navigateur)
   */
  private showSystemNotification(notification: NotificationResponse): void {
    if ('Notification' in window && Notification.permission === 'granted') {
      new Notification(notification.titre, {
        body: notification.message,
        icon: '/assets/icons/notification-icon.png'
      });
    }
  }

  // ===== PUBLIC METHODS =====

  /**
   * 🔹 Demander la permission pour les notifications système
   */
  requestNotificationPermission(): Promise<NotificationPermission> {
    if ('Notification' in window) {
      return Notification.requestPermission();
    }
    return Promise.resolve('denied');
  }

  /**
   * 🔹 Marquer une notification comme lue et mettre à jour l'état local
   */
  markNotificationAsRead(notificationId: number): void {
    this.markAsRead(notificationId).subscribe({
      next: () => {
        const notifications = this.notificationsSubject.value.map(n => 
          n.id === notificationId ? { ...n, lue: true, dateLecture: new Date() } : n
        );
        this.notificationsSubject.next(notifications);
        this.updateUnreadCount(notifications);
      },
      error: (error) => {
        console.error('❌ [NOTIFICATION SERVICE] Erreur marquage lecture:', error);
      }
    });
  }

  /**
   * 🔹 Marquer toutes les notifications comme lues et mettre à jour l'état local
   */
  markAllNotificationsAsRead(): void {
    this.markAllAsRead().subscribe({
      next: () => {
        const notifications = this.notificationsSubject.value.map(n => 
          ({ ...n, lue: true, dateLecture: new Date() })
        );
        this.notificationsSubject.next(notifications);
        this.updateUnreadCount(notifications);
      },
      error: (error) => {
        console.error('❌ [NOTIFICATION SERVICE] Erreur marquage toutes lues:', error);
      }
    });
  }

  /**
   * 🔹 Supprimer une notification et mettre à jour l'état local
   */
  removeNotification(notificationId: number): void {
    this.deleteNotification(notificationId).subscribe({
      next: () => {
        const notifications = this.notificationsSubject.value.filter(n => n.id !== notificationId);
        this.notificationsSubject.next(notifications);
        this.updateUnreadCount(notifications);
      },
      error: (error) => {
        console.error('❌ [NOTIFICATION SERVICE] Erreur suppression:', error);
      }
    });
  }

  /**
   * 🔹 Obtenir le nombre de notifications non lues
   */
  getUnreadCount(): number {
    return this.unreadCountSubject.value;
  }

  /**
   * 🔹 Obtenir les notifications actuelles
   */
  getCurrentNotifications(): NotificationResponse[] {
    return this.notificationsSubject.value;
  }

  /**
   * 🔹 Rafraîchir les notifications
   */
  refreshNotifications(): void {
    this.loadNotifications();
  }

  // ===== UTILITY METHODS =====

  /**
   * 🔹 Obtenir l'icône selon le type de notification
   */
  getNotificationIcon(type: NotificationType): string {
    const icons = {
      [NotificationType.INFO]: 'info',
      [NotificationType.SUCCESS]: 'check_circle',
      [NotificationType.WARNING]: 'warning',
      [NotificationType.ERROR]: 'error',
      [NotificationType.REMINDER]: 'schedule'
    };
    return icons[type] || 'notifications';
  }

  /**
   * 🔹 Obtenir la couleur selon le type de notification
   */
  getNotificationColor(type: NotificationType): string {
    const colors = {
      [NotificationType.INFO]: 'blue',
      [NotificationType.SUCCESS]: 'green',
      [NotificationType.WARNING]: 'orange',
      [NotificationType.ERROR]: 'red',
      [NotificationType.REMINDER]: 'purple'
    };
    return colors[type] || 'gray';
  }

  // ===== UI NOTIFICATIONS (TOASTS) =====

  /**
   * 🔹 Afficher une notification d'erreur
   */
  showError(message: string, title: string = 'Erreur', duration: number = 5000): void {
    this.showUINotification('error', title, message, duration);
  }

  /**
   * 🔹 Afficher une notification de succès
   */
  showSuccess(message: string, title: string = 'Succès', duration: number = 3000): void {
    this.showUINotification('success', title, message, duration);
  }

  /**
   * 🔹 Afficher une notification d'avertissement
   */
  showWarning(message: string, title: string = 'Attention', duration: number = 4000): void {
    this.showUINotification('warning', title, message, duration);
  }

  /**
   * 🔹 Afficher une notification d'information
   */
  showInfo(message: string, title: string = 'Information', duration: number = 3000): void {
    this.showUINotification('info', title, message, duration);
  }

  /**
   * 🔹 Afficher une notification UI générique
   */
  private showUINotification(
    type: 'success' | 'error' | 'warning' | 'info',
    title: string,
    message: string,
    duration: number = 3000
  ): void {
    const notification: UINotification = {
      id: this.generateNotificationId(),
      type,
      title,
      message,
      duration,
      timestamp: new Date()
    };

    const currentNotifications = this.uiNotificationsSubject.value;
    this.uiNotificationsSubject.next([...currentNotifications, notification]);

    // Auto-remove après la durée spécifiée
    if (duration > 0) {
      setTimeout(() => {
        this.removeUINotification(notification.id);
      }, duration);
    }

    console.log(`🔔 [NOTIFICATION SERVICE] ${type.toUpperCase()}: ${title} - ${message}`);
  }

  /**
   * 🔹 Supprimer une notification UI
   */
  removeUINotification(notificationId: string): void {
    const currentNotifications = this.uiNotificationsSubject.value;
    const filteredNotifications = currentNotifications.filter(n => n.id !== notificationId);
    this.uiNotificationsSubject.next(filteredNotifications);
  }

  /**
   * 🔹 Supprimer toutes les notifications UI
   */
  clearAllUINotifications(): void {
    this.uiNotificationsSubject.next([]);
  }

  /**
   * 🔹 Générer un ID unique pour les notifications UI
   */
  private generateNotificationId(): string {
    return `notification_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
  }

  /**
   * 🔹 Nettoyer les ressources lors de la destruction du service
   */
  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.disconnectWebSocket();
  }
}