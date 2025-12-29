import { ErrorHandler, Injectable, inject } from '@angular/core';
import { NotificationService } from '../services/notification.service';
import { environment } from '../../environments/environment';

@Injectable()
export class GlobalErrorHandler implements ErrorHandler {
  private notificationService = inject(NotificationService);

  handleError(error: any): void {
    console.error('🔥 [GLOBAL ERROR HANDLER] Erreur globale capturée:', error);

    // Log détaillé en développement
    if (!environment.production) {
      console.group('🔍 Détails de l\'erreur:');
      console.error('Message:', error.message);
      console.error('Stack:', error.stack);
      console.error('Objet complet:', error);
      console.groupEnd();
    }

    // Déterminer le type d'erreur et le message approprié
    let userMessage = 'Une erreur inattendue s\'est produite.';
    let shouldShowNotification = true;

    // Erreurs Angular spécifiques
    if (error.name === 'ChunkLoadError') {
      userMessage = 'Erreur de chargement de l\'application. Veuillez rafraîchir la page.';
    } else if (error.message?.includes('Loading chunk')) {
      userMessage = 'Erreur de chargement. Veuillez rafraîchir la page.';
    } else if (error.message?.includes('Script error')) {
      userMessage = 'Erreur de script. Veuillez rafraîchir la page.';
    } else if (error.message?.includes('Network Error')) {
      userMessage = 'Erreur de connexion réseau. Vérifiez votre connexion internet.';
    } else if (error.message?.includes('Failed to fetch')) {
      userMessage = 'Impossible de contacter le serveur. Veuillez réessayer.';
    } else if (error.name === 'TypeError' && error.message?.includes('Cannot read property')) {
      userMessage = 'Erreur de données. Veuillez rafraîchir la page.';
    } else if (error.name === 'ReferenceError') {
      userMessage = 'Erreur de référence. Veuillez rafraîchir la page.';
    } else if (error.rejection) {
      // Promise rejection non gérée
      userMessage = 'Erreur de traitement. Veuillez réessayer.';
      console.error('🔥 Promise rejection non gérée:', error.rejection);
    }

    // Erreurs HTTP déjà gérées par l'interceptor
    if (error.status && typeof error.status === 'number') {
      shouldShowNotification = false;
    }

    // Erreurs de développement (ne pas notifier l'utilisateur)
    if (!environment.production && (
      error.message?.includes('ExpressionChangedAfterItHasBeenCheckedError') ||
      error.message?.includes('NG0100') ||
      error.message?.includes('NG0200')
    )) {
      shouldShowNotification = false;
    }

    // Afficher la notification à l'utilisateur
    if (shouldShowNotification) {
      this.notificationService.showError(userMessage, 'Erreur système');
    }

    // Log vers un service de monitoring en production (optionnel)
    if (environment.production) {
      this.logToMonitoringService(error);
    }

    // Rethrow l'erreur pour maintenir le comportement par défaut d'Angular
    // (utile pour les outils de développement)
    if (!environment.production) {
      throw error;
    }
  }

  /**
   * 📊 Logger l'erreur vers un service de monitoring externe
   */
  private logToMonitoringService(error: any): void {
    try {
      // Ici vous pouvez intégrer avec des services comme:
      // - Sentry
      // - LogRocket
      // - Bugsnag
      // - Application Insights
      
      const errorInfo = {
        message: error.message || 'Unknown error',
        stack: error.stack,
        url: window.location.href,
        userAgent: navigator.userAgent,
        timestamp: new Date().toISOString(),
        userId: this.getCurrentUserId(),
        sessionId: this.getSessionId()
      };

      console.log('📊 [GLOBAL ERROR HANDLER] Erreur à logger:', errorInfo);
      
      // Exemple d'envoi vers un endpoint de logging
      // fetch('/api/errors', {
      //   method: 'POST',
      //   headers: { 'Content-Type': 'application/json' },
      //   body: JSON.stringify(errorInfo)
      // }).catch(logError => {
      //   console.error('Impossible de logger l\'erreur:', logError);
      // });
      
    } catch (loggingError) {
      console.error('🔥 [GLOBAL ERROR HANDLER] Erreur lors du logging:', loggingError);
    }
  }

  /**
   * 👤 Obtenir l'ID de l'utilisateur actuel (si connecté)
   */
  private getCurrentUserId(): string | null {
    try {
      const token = localStorage.getItem(environment.tokenKey);
      if (token) {
        const payload = JSON.parse(atob(token.split('.')[1]));
        return payload.sub || payload.userId || null;
      }
    } catch {
      // Ignore les erreurs de parsing du token
    }
    return null;
  }

  /**
   * 🔑 Obtenir l'ID de session
   */
  private getSessionId(): string {
    let sessionId = sessionStorage.getItem('sessionId');
    if (!sessionId) {
      sessionId = `session_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
      sessionStorage.setItem('sessionId', sessionId);
    }
    return sessionId;
  }
}