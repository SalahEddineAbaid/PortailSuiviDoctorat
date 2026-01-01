import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';
import { catchError, switchMap, throwError } from 'rxjs';
import { Router } from '@angular/router';

/**
 * 🔐 Intercepteur d'authentification JWT
 * 
 * Fonctionnalités :
 * - Ajoute automatiquement le token JWT aux requêtes
 * - Gère le rafraîchissement automatique du token en cas d'expiration
 * - Redirige vers login en cas d'échec d'authentification
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  // ✅ Ne pas ajouter le token pour les requêtes d'authentification
  const isAuthRequest = req.url.includes('/auth/login') || 
                        req.url.includes('/auth/register') ||
                        req.url.includes('/auth/refresh');

  if (isAuthRequest) {
    return next(req);
  }

  // ✅ Récupérer le token
  const token = authService.getToken();

  // ✅ Cloner la requête et ajouter le token si disponible
  let authReq = req;
  if (token) {
    authReq = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
  }

  // ✅ Envoyer la requête et gérer les erreurs 401
  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      // Si erreur 401 et qu'on a un refresh token, tenter le rafraîchissement
      if (error.status === 401 && authService.getRefreshToken()) {
        console.log('🔄 Token expiré, tentative de rafraîchissement...');
        
        return authService.refreshToken().pipe(
          switchMap(() => {
            // ✅ Réessayer la requête avec le nouveau token
            const newToken = authService.getToken();
            const retryReq = req.clone({
              setHeaders: {
                Authorization: `Bearer ${newToken}`
              }
            });
            
            console.log('✅ Token rafraîchi, nouvelle tentative de requête');
            return next(retryReq);
          }),
          catchError((refreshError: any) => {
            // ✅ Échec du rafraîchissement, déconnecter l'utilisateur
            console.error('❌ Échec du rafraîchissement du token');
            authService.logout();
            return throwError(() => refreshError);
          })
        );
      }

      // Pour toutes les autres erreurs, les laisser passer
      return throwError(() => error);
    })
  );
};
