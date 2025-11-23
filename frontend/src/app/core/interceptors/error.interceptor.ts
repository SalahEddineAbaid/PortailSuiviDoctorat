import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError, switchMap } from 'rxjs';
import { AuthService } from '../services/auth.service';

/**
 * 🔴 Interceptor qui gère les erreurs HTTP globalement
 */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      console.error('❌ Erreur HTTP:', error);

      // Erreur 401 : Token expiré ou invalide
      if (error.status === 401) {
        // Si c'est déjà une requête de refresh, déconnecter
        if (req.url.includes('/auth/refresh')) {
          console.error('❌ Refresh token invalide, déconnexion');
          authService.logout();
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
            const token = authService.getToken(); // ✅ Changé de getAccessToken() à getToken()
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
            return throwError(() => refreshError);
          })
        );
      }

      // Erreur 403 : Accès refusé
      if (error.status === 403) {
        console.error('❌ Accès refusé (403)');
        router.navigate(['/login'], {
          queryParams: { forbidden: 'true' }
        });
      }

      // Erreur 404 : Ressource non trouvée
      if (error.status === 404) {
        console.error('❌ Ressource non trouvée (404):', req.url);
      }

      // Erreur 500 : Erreur serveur
      if (error.status === 500) {
        console.error('❌ Erreur serveur (500)');
      }

      // Erreur réseau (pas de connexion)
      if (error.status === 0) {
        console.error('❌ Erreur réseau : Backend inaccessible sur', req.url);
      }

      return throwError(() => error);
    })
  );
};