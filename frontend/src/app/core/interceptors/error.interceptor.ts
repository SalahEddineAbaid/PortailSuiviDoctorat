import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { environment } from '../../environments/environment';

/**
 * 🔴 Interceptor qui gère les erreurs HTTP globalement
 * ⚠️ N'injecte PAS AuthService ni NotificationService pour éviter les dépendances circulaires
 */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      // ✅ Ne pas logger les erreurs 404 pour les services optionnels (notifications, etc.)
      const isOptionalService = req.url.includes('/notifications') || 
                                req.url.includes('/websocket');
      
      if (!isOptionalService) {
        console.error('❌ Erreur HTTP:', error.status, req.url);
      }

      // Erreur 401 : Token expiré ou invalide
      if (error.status === 401) {
        // Si c'est une requête d'authentification (login/register/refresh), laisser passer
        if (req.url.includes('/auth/login') || 
            req.url.includes('/auth/register') ||
            req.url.includes('/auth/refresh')) {
          return throwError(() => error);
        }

        // Sinon, déconnecter et rediriger
        console.warn('⚠️ Token expiré ou invalide, déconnexion...');
        localStorage.removeItem(environment.tokenKey);
        localStorage.removeItem(environment.refreshTokenKey);
        router.navigate(['/login'], {
          queryParams: { expired: 'true' }
        });
      }

      // Erreur 403 : Accès refusé
      if (error.status === 403) {
        console.error('❌ Accès refusé (403)');
        router.navigate(['/login'], {
          queryParams: { forbidden: 'true' }
        });
      }

      return throwError(() => error);
    })
  );
};