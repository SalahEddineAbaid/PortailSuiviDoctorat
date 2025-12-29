import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError, switchMap } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { NotificationService } from '../services/notification.service';

/**
 * 🔴 Interceptor qui gère les erreurs HTTP globalement
 */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const notificationService = inject(NotificationService);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      console.error('❌ Erreur HTTP:', error);

      // Erreur 401 : Token expiré ou invalide
      if (error.status === 401) {
        // Si c'est déjà une requête de refresh, déconnecter
        if (req.url.includes('/auth/refresh')) {
          console.error('❌ Refresh token invalide, déconnexion');
          authService.logout();
          notificationService.showError('Session expirée. Veuillez vous reconnecter.');
          return throwError(() => error);
        }

        // Si c'est une requête d'authentification (login/register), ne pas tenter de refresh
        if (req.url.includes('/auth/login') || req.url.includes('/auth/register')) {
          console.log('ℹ️ Erreur 401 sur route d\'authentification, pas de refresh');
          return throwError(() => error);
        }

        // Sinon, essayer de rafraîchir le token
        console.warn('⚠️ Token expiré, tentative de refresh...');
        return authService.refreshToken().pipe(
          switchMap(() => {
            // ✅ Retry la requête avec le nouveau token
            const token = authService.getToken();
            const clonedRequest = req.clone({
              setHeaders: {
                Authorization: `Bearer ${token}`
              }
            });
            console.log('✅ Token rafraîchi, retry de la requête');
            return next(clonedRequest);
          }),
          catchError((refreshError) => {
            console.error('❌ Impossible de rafraîchir le token');
            authService.logout();
            router.navigate(['/login'], {
              queryParams: { expired: 'true' }
            });
            notificationService.showError('Session expirée. Veuillez vous reconnecter.');
            return throwError(() => refreshError);
          })
        );
      }

      // Erreur 400 : Données invalides
      if (error.status === 400) {
        console.error('❌ Données invalides (400)');
        const message = error.error?.message || 'Données invalides. Veuillez vérifier votre saisie.';
        notificationService.showError(message);
      }

      // Erreur 403 : Accès refusé
      if (error.status === 403) {
        console.error('❌ Accès refusé (403)');
        router.navigate(['/login'], {
          queryParams: { forbidden: 'true' }
        });
        notificationService.showError('Accès refusé. Vous n\'avez pas les permissions nécessaires.');
      }

      // Erreur 404 : Ressource non trouvée
      if (error.status === 404) {
        console.error('❌ Ressource non trouvée (404):', req.url);
        notificationService.showError('Ressource non trouvée.');
      }

      // Erreur 409 : Conflit de données
      if (error.status === 409) {
        console.error('❌ Conflit de données (409)');
        const message = error.error?.message || 'Conflit de données. Cette action ne peut être effectuée.';
        notificationService.showError(message);
      }

      // Erreur 422 : Données non valides
      if (error.status === 422) {
        console.error('❌ Données non valides (422)');
        const message = error.error?.message || 'Données non valides. Veuillez corriger les erreurs.';
        notificationService.showError(message);
      }

      // Erreur 500 : Erreur serveur
      if (error.status === 500) {
        console.error('❌ Erreur serveur (500)');
        notificationService.showError('Erreur serveur. Veuillez réessayer plus tard.');
      }

      // Erreur 502 : Bad Gateway
      if (error.status === 502) {
        console.error('❌ Bad Gateway (502)');
        notificationService.showError('Service temporairement indisponible. Veuillez réessayer.');
      }

      // Erreur 503 : Service indisponible
      if (error.status === 503) {
        console.error('❌ Service indisponible (503)');
        notificationService.showError('Service en maintenance. Veuillez réessayer plus tard.');
      }

      // Erreur réseau (pas de connexion)
      if (error.status === 0) {
        console.error('❌ Erreur réseau : Backend inaccessible sur', req.url);
        notificationService.showError('Erreur de connexion. Vérifiez votre connexion internet.');
      }

      // Timeout ou autres erreurs spécifiques
      if (error.message?.includes('timeout') || error.message?.includes('Timeout')) {
        console.error('❌ Timeout de la requête');
        notificationService.showError('La requête a pris trop de temps. Veuillez réessayer.');
      }

      return throwError(() => error);
    })
  );
};