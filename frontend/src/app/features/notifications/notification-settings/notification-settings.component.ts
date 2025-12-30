import { Component, OnInit, OnDestroy, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule, AsyncPipe } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Observable, Subject } from 'rxjs';
import { takeUntil, finalize } from 'rxjs/operators';

import { NotificationService } from '../../../core/services/notification.service';
import { WebSocketService, WebSocketState } from '../../../core/services/websocket.service';
import { NotificationSettings } from '../../../core/models/notification.model';

@Component({
  selector: 'app-notification-settings',
  standalone: true,
  imports: [CommonModule, AsyncPipe, ReactiveFormsModule],
  templateUrl: './notification-settings.component.html',
  styleUrls: ['./notification-settings.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class NotificationSettingsComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();

  settingsForm!: FormGroup;
  loading = false;
  saving = false;
  
  // État des notifications système
  systemNotificationPermission: NotificationPermission = 'default';
  webSocketState$: Observable<WebSocketState>;
  
  // Options de configuration
  readonly notificationChannels = [
    {
      key: 'emailEnabled',
      label: 'Notifications par email',
      description: 'Recevoir les notifications importantes par email',
      icon: 'email'
    },
    {
      key: 'pushEnabled',
      label: 'Notifications push',
      description: 'Recevoir les notifications en temps réel dans le navigateur',
      icon: 'notifications'
    }
  ];

  readonly notificationTypes = [
    {
      key: 'inscriptionNotifications',
      label: 'Notifications d\'inscription',
      description: 'Alertes concernant les inscriptions et réinscriptions',
      icon: 'school'
    },
    {
      key: 'soutenanceNotifications',
      label: 'Notifications de soutenance',
      description: 'Alertes concernant les demandes et validations de soutenance',
      icon: 'event'
    },
    {
      key: 'adminNotifications',
      label: 'Notifications administratives',
      description: 'Alertes concernant les validations et décisions administratives',
      icon: 'admin_panel_settings'
    }
  ];

  readonly WebSocketState = WebSocketState;

  constructor(
    private fb: FormBuilder,
    private notificationService: NotificationService,
    private webSocketService: WebSocketService,
    private cdr: ChangeDetectorRef
  ) {
    this.createForm();
    this.webSocketState$ = this.webSocketService.state$;
  }

  ngOnInit(): void {
    this.loadSettings();
    this.checkSystemNotificationPermission();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * 🔹 Créer le formulaire de paramètres
   */
  private createForm(): void {
    this.settingsForm = this.fb.group({
      emailEnabled: [true],
      pushEnabled: [true],
      inscriptionNotifications: [true],
      soutenanceNotifications: [true],
      adminNotifications: [true]
    });
  }

  /**
   * 🔹 Charger les paramètres actuels
   */
  private loadSettings(): void {
    this.loading = true;
    this.cdr.detectChanges();

    this.notificationService.getNotificationSettings()
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => {
          this.loading = false;
          this.cdr.detectChanges();
        })
      )
      .subscribe({
        next: (settings) => {
          console.log('✅ [NOTIFICATION SETTINGS] Paramètres chargés:', settings);
          this.settingsForm.patchValue(settings);
          this.cdr.detectChanges();
        },
        error: (error) => {
          console.error('❌ [NOTIFICATION SETTINGS] Erreur chargement:', error);
          this.notificationService.showError('Erreur lors du chargement des paramètres');
        }
      });
  }

  /**
   * 🔹 Sauvegarder les paramètres
   */
  onSaveSettings(): void {
    if (this.settingsForm.invalid) {
      this.notificationService.showWarning('Veuillez corriger les erreurs du formulaire');
      return;
    }

    this.saving = true;
    this.cdr.detectChanges();

    const settings: NotificationSettings = this.settingsForm.value;

    this.notificationService.updateNotificationSettings(settings)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => {
          this.saving = false;
          this.cdr.detectChanges();
        })
      )
      .subscribe({
        next: (updatedSettings) => {
          console.log('✅ [NOTIFICATION SETTINGS] Paramètres sauvegardés:', updatedSettings);
          this.notificationService.showSuccess('Paramètres sauvegardés avec succès');
          
          // Reconnecter WebSocket si les notifications push ont été activées
          if (updatedSettings.pushEnabled && !this.webSocketService.isConnected()) {
            this.connectWebSocket();
          } else if (!updatedSettings.pushEnabled && this.webSocketService.isConnected()) {
            this.disconnectWebSocket();
          }
        },
        error: (error) => {
          console.error('❌ [NOTIFICATION SETTINGS] Erreur sauvegarde:', error);
          this.notificationService.showError('Erreur lors de la sauvegarde des paramètres');
        }
      });
  }

  /**
   * 🔹 Réinitialiser le formulaire
   */
  onResetForm(): void {
    this.loadSettings();
    this.notificationService.showInfo('Paramètres réinitialisés');
  }

  /**
   * 🔹 Vérifier la permission des notifications système
   */
  private checkSystemNotificationPermission(): void {
    if ('Notification' in window) {
      this.systemNotificationPermission = Notification.permission;
    }
  }

  /**
   * 🔹 Demander la permission pour les notifications système
   */
  async requestNotificationPermission(): Promise<void> {
    if (!('Notification' in window)) {
      this.notificationService.showWarning('Les notifications ne sont pas supportées par ce navigateur');
      return;
    }

    try {
      const permission = await this.notificationService.requestNotificationPermission();
      this.systemNotificationPermission = permission;
      this.cdr.detectChanges();

      if (permission === 'granted') {
        this.notificationService.showSuccess('Permission accordée pour les notifications');
        
        // Activer automatiquement les notifications push
        this.settingsForm.patchValue({ pushEnabled: true });
      } else if (permission === 'denied') {
        this.notificationService.showWarning('Permission refusée pour les notifications');
        
        // Désactiver les notifications push
        this.settingsForm.patchValue({ pushEnabled: false });
      }
    } catch (error) {
      console.error('❌ [NOTIFICATION SETTINGS] Erreur permission:', error);
      this.notificationService.showError('Erreur lors de la demande de permission');
    }
  }

  /**
   * 🔹 Connecter WebSocket
   */
  connectWebSocket(): void {
    this.notificationService.connectWebSocket();
    this.notificationService.showInfo('Connexion aux notifications temps réel...');
  }

  /**
   * 🔹 Déconnecter WebSocket
   */
  disconnectWebSocket(): void {
    this.notificationService.disconnectWebSocket();
    this.notificationService.showInfo('Déconnexion des notifications temps réel');
  }

  /**
   * 🔹 Tester les notifications
   */
  testNotification(): void {
    if (this.systemNotificationPermission === 'granted') {
      // Notification système
      new Notification('Test de notification', {
        body: 'Ceci est un test de notification système',
        icon: '/assets/icons/notification-icon.png'
      });
    }
    
    // Notification UI
    this.notificationService.showInfo('Ceci est un test de notification dans l\'interface', 'Test');
  }

  /**
   * 🔹 Obtenir le texte d'état WebSocket
   */
  getWebSocketStateText(state: WebSocketState): string {
    const stateTexts = {
      [WebSocketState.CONNECTING]: 'Connexion en cours...',
      [WebSocketState.CONNECTED]: 'Connecté',
      [WebSocketState.DISCONNECTED]: 'Déconnecté',
      [WebSocketState.RECONNECTING]: 'Reconnexion en cours...',
      [WebSocketState.ERROR]: 'Erreur de connexion'
    };
    return stateTexts[state] || 'État inconnu';
  }

  /**
   * 🔹 Obtenir la couleur d'état WebSocket
   */
  getWebSocketStateColor(state: WebSocketState): string {
    const stateColors = {
      [WebSocketState.CONNECTING]: 'orange',
      [WebSocketState.CONNECTED]: 'green',
      [WebSocketState.DISCONNECTED]: 'gray',
      [WebSocketState.RECONNECTING]: 'orange',
      [WebSocketState.ERROR]: 'red'
    };
    return stateColors[state] || 'gray';
  }

  /**
   * 🔹 Obtenir le texte de permission des notifications
   */
  getPermissionText(): string {
    const permissionTexts = {
      'default': 'Non demandée',
      'granted': 'Accordée',
      'denied': 'Refusée'
    };
    return permissionTexts[this.systemNotificationPermission] || 'Inconnue';
  }

  /**
   * 🔹 Obtenir la couleur de permission des notifications
   */
  getPermissionColor(): string {
    const permissionColors = {
      'default': 'gray',
      'granted': 'green',
      'denied': 'red'
    };
    return permissionColors[this.systemNotificationPermission] || 'gray';
  }

  /**
   * 🔹 Vérifier si le formulaire a été modifié
   */
  isFormDirty(): boolean {
    return this.settingsForm.dirty;
  }

  /**
   * 🔹 Vérifier si les notifications push sont disponibles
   */
  isPushNotificationAvailable(): boolean {
    return 'Notification' in window && this.systemNotificationPermission === 'granted';
  }
}