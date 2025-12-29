import { Injectable, OnDestroy } from '@angular/core';
import { Observable, Subject, BehaviorSubject, timer } from 'rxjs';
import { takeUntil, switchMap, retryWhen, delay, tap } from 'rxjs/operators';
import { environment } from '../../environments/environment';

export interface WebSocketConfig {
  url: string;
  protocols?: string | string[];
  reconnectInterval?: number;
  maxReconnectAttempts?: number;
  enableHeartbeat?: boolean;
  heartbeatInterval?: number;
}

export interface WebSocketMessage {
  type: string;
  data: any;
  timestamp?: Date;
  id?: string;
}

export enum WebSocketState {
  CONNECTING = 'CONNECTING',
  CONNECTED = 'CONNECTED',
  DISCONNECTED = 'DISCONNECTED',
  RECONNECTING = 'RECONNECTING',
  ERROR = 'ERROR'
}

@Injectable({
  providedIn: 'root'
})
export class WebSocketService implements OnDestroy {
  private socket: WebSocket | null = null;
  private destroy$ = new Subject<void>();
  
  // Configuration par défaut
  private defaultConfig: WebSocketConfig = {
    url: '',
    reconnectInterval: 3000,
    maxReconnectAttempts: 5,
    enableHeartbeat: true,
    heartbeatInterval: 30000
  };

  // Subjects pour la gestion des états et messages
  private messagesSubject = new Subject<WebSocketMessage>();
  private stateSubject = new BehaviorSubject<WebSocketState>(WebSocketState.DISCONNECTED);
  private errorSubject = new Subject<Event>();

  // Compteurs et timers
  private reconnectAttempts = 0;
  private reconnectTimer: any;
  private heartbeatTimer: any;
  private currentConfig: WebSocketConfig | null = null;

  // Observables publics
  public messages$ = this.messagesSubject.asObservable();
  public state$ = this.stateSubject.asObservable();
  public errors$ = this.errorSubject.asObservable();

  constructor() {
    console.log('🔌 [WEBSOCKET SERVICE] Service initialisé');
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.disconnect();
  }

  /**
   * 🔹 Se connecter au WebSocket
   */
  connect(config: Partial<WebSocketConfig>): Observable<WebSocketState> {
    this.currentConfig = { ...this.defaultConfig, ...config };
    
    if (!this.currentConfig.url) {
      console.error('❌ [WEBSOCKET SERVICE] URL manquante');
      this.stateSubject.next(WebSocketState.ERROR);
      return this.state$;
    }

    // Fermer la connexion existante si elle existe
    if (this.socket) {
      this.disconnect();
    }

    console.log('🔌 [WEBSOCKET SERVICE] Connexion à:', this.currentConfig.url);
    this.stateSubject.next(WebSocketState.CONNECTING);

    try {
      this.socket = new WebSocket(this.currentConfig.url, this.currentConfig.protocols);
      this.setupEventHandlers();
    } catch (error) {
      console.error('❌ [WEBSOCKET SERVICE] Erreur création WebSocket:', error);
      this.stateSubject.next(WebSocketState.ERROR);
      this.handleReconnect();
    }

    return this.state$;
  }

  /**
   * 🔹 Se connecter avec authentification JWT
   */
  connectWithAuth(baseUrl: string, token: string, additionalConfig?: Partial<WebSocketConfig>): Observable<WebSocketState> {
    const wsUrl = `${baseUrl}?token=${encodeURIComponent(token)}`;
    
    return this.connect({
      url: wsUrl,
      ...additionalConfig
    });
  }

  /**
   * 🔹 Se déconnecter du WebSocket
   */
  disconnect(): void {
    console.log('🔌 [WEBSOCKET SERVICE] Déconnexion');
    
    // Nettoyer les timers
    this.clearTimers();
    
    // Fermer la connexion
    if (this.socket) {
      this.socket.close(1000, 'Déconnexion volontaire');
      this.socket = null;
    }
    
    // Réinitialiser les compteurs
    this.reconnectAttempts = 0;
    this.stateSubject.next(WebSocketState.DISCONNECTED);
  }

  /**
   * 🔹 Envoyer un message
   */
  send(message: WebSocketMessage): boolean {
    if (!this.socket || this.socket.readyState !== WebSocket.OPEN) {
      console.warn('⚠️ [WEBSOCKET SERVICE] WebSocket non connecté');
      return false;
    }

    try {
      const messageWithTimestamp = {
        ...message,
        timestamp: new Date(),
        id: this.generateMessageId()
      };
      
      this.socket.send(JSON.stringify(messageWithTimestamp));
      console.log('📤 [WEBSOCKET SERVICE] Message envoyé:', messageWithTimestamp);
      return true;
    } catch (error) {
      console.error('❌ [WEBSOCKET SERVICE] Erreur envoi message:', error);
      return false;
    }
  }

  /**
   * 🔹 Envoyer un ping (heartbeat)
   */
  ping(): boolean {
    return this.send({
      type: 'PING',
      data: { timestamp: Date.now() }
    });
  }

  /**
   * 🔹 Obtenir l'état actuel de la connexion
   */
  getState(): WebSocketState {
    return this.stateSubject.value;
  }

  /**
   * 🔹 Vérifier si la connexion est active
   */
  isConnected(): boolean {
    return this.socket?.readyState === WebSocket.OPEN && 
           this.stateSubject.value === WebSocketState.CONNECTED;
  }

  /**
   * 🔹 Obtenir les statistiques de connexion
   */
  getConnectionStats(): {
    state: WebSocketState;
    reconnectAttempts: number;
    maxReconnectAttempts: number;
    url: string | null;
  } {
    return {
      state: this.stateSubject.value,
      reconnectAttempts: this.reconnectAttempts,
      maxReconnectAttempts: this.currentConfig?.maxReconnectAttempts || 0,
      url: this.currentConfig?.url || null
    };
  }

  // ===== MÉTHODES PRIVÉES =====

  /**
   * 🔹 Configurer les gestionnaires d'événements WebSocket
   */
  private setupEventHandlers(): void {
    if (!this.socket) return;

    this.socket.onopen = (event) => {
      console.log('✅ [WEBSOCKET SERVICE] Connexion établie');
      this.reconnectAttempts = 0;
      this.stateSubject.next(WebSocketState.CONNECTED);
      
      // Démarrer le heartbeat si activé
      if (this.currentConfig?.enableHeartbeat) {
        this.startHeartbeat();
      }

      // Émettre un message de connexion
      this.messagesSubject.next({
        type: 'SYSTEM_CONNECTED',
        data: { event, timestamp: new Date() }
      });
    };

    this.socket.onmessage = (event) => {
      try {
        const message = JSON.parse(event.data);
        console.log('📨 [WEBSOCKET SERVICE] Message reçu:', message);
        
        // Gérer les messages système
        if (message.type === 'PONG') {
          console.log('🏓 [WEBSOCKET SERVICE] Pong reçu');
          return;
        }

        // Ajouter timestamp si manquant
        if (!message.timestamp) {
          message.timestamp = new Date();
        }

        this.messagesSubject.next(message);
      } catch (error) {
        console.error('❌ [WEBSOCKET SERVICE] Erreur parsing message:', error);
        this.messagesSubject.next({
          type: 'PARSE_ERROR',
          data: { error, rawData: event.data },
          timestamp: new Date()
        });
      }
    };

    this.socket.onclose = (event) => {
      console.log('🔌 [WEBSOCKET SERVICE] Connexion fermée:', event.code, event.reason);
      this.clearTimers();
      this.socket = null;
      
      // Émettre un message de déconnexion
      this.messagesSubject.next({
        type: 'SYSTEM_DISCONNECTED',
        data: { event, timestamp: new Date() }
      });

      // Tenter une reconnexion si ce n'est pas une fermeture volontaire
      if (event.code !== 1000) {
        this.stateSubject.next(WebSocketState.DISCONNECTED);
        this.handleReconnect();
      }
    };

    this.socket.onerror = (event) => {
      console.error('❌ [WEBSOCKET SERVICE] Erreur WebSocket:', event);
      this.stateSubject.next(WebSocketState.ERROR);
      this.errorSubject.next(event);
      
      // Émettre un message d'erreur
      this.messagesSubject.next({
        type: 'SYSTEM_ERROR',
        data: { event, timestamp: new Date() }
      });
    };
  }

  /**
   * 🔹 Gérer la reconnexion automatique
   */
  private handleReconnect(): void {
    if (!this.currentConfig) return;

    const maxAttempts = this.currentConfig.maxReconnectAttempts || 0;
    
    if (this.reconnectAttempts >= maxAttempts) {
      console.error('❌ [WEBSOCKET SERVICE] Nombre maximum de tentatives de reconnexion atteint');
      this.stateSubject.next(WebSocketState.ERROR);
      return;
    }

    this.reconnectAttempts++;
    this.stateSubject.next(WebSocketState.RECONNECTING);
    
    const delay = this.calculateReconnectDelay();
    console.log(`🔄 [WEBSOCKET SERVICE] Tentative de reconnexion ${this.reconnectAttempts}/${maxAttempts} dans ${delay}ms`);
    
    this.reconnectTimer = setTimeout(() => {
      if (this.currentConfig) {
        this.connect(this.currentConfig);
      }
    }, delay);
  }

  /**
   * 🔹 Calculer le délai de reconnexion (exponential backoff)
   */
  private calculateReconnectDelay(): number {
    const baseDelay = this.currentConfig?.reconnectInterval || 3000;
    const maxDelay = 30000; // 30 secondes maximum
    
    const exponentialDelay = baseDelay * Math.pow(2, this.reconnectAttempts - 1);
    return Math.min(exponentialDelay, maxDelay);
  }

  /**
   * 🔹 Démarrer le heartbeat
   */
  private startHeartbeat(): void {
    if (!this.currentConfig?.enableHeartbeat) return;

    const interval = this.currentConfig.heartbeatInterval || 30000;
    
    this.heartbeatTimer = setInterval(() => {
      if (this.isConnected()) {
        this.ping();
      } else {
        this.clearTimers();
      }
    }, interval);
  }

  /**
   * 🔹 Nettoyer les timers
   */
  private clearTimers(): void {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }
    
    if (this.heartbeatTimer) {
      clearInterval(this.heartbeatTimer);
      this.heartbeatTimer = null;
    }
  }

  /**
   * 🔹 Générer un ID unique pour les messages
   */
  private generateMessageId(): string {
    return `msg_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
  }
}