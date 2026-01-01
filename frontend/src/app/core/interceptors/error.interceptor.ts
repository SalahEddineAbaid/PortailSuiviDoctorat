import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { environment } from '../../environments/environment';

/**
 * 🔴 Interceptor qui gère les erreurs HTTP globalement
 * ⚠️ N'injecte PAS AuthService ni NotificationService pour éviter les dépendances circulaires
 * ⚠️ Les erreurs 401 sont gérées par authInterceptor, pas ici
 */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      // ✅ Ne pas logger les erreurs 404 pour les services optionnels (notifications, etc.)
      const isOptionalService = req.url.includes('/notifications') || 
                                req.url.includes('/websocket');
      
      if (!isOptionalService && error.status !== 401) {
        console.error('❌ Erreur HTTP:', error.status, req.url);
      }

      // ⚠️ Erreur 401 : Laissée à authInterceptor pour gérer le refresh token
      // Ne pas déconnecter ici car authInterceptor va tenter le refresh
      if (error.status === 401) {
        // Juste logger, ne pas déconnecter
        console.warn('⚠️ [ERROR INTERCEPTOR] Erreur 401 détectée, laissée à authInterceptor');
        return throwError(() => error);
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