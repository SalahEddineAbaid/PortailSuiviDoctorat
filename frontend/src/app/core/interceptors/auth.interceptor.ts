import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { environment } from '../../environments/environment';

/**
 * 🔐 Interceptor qui ajoute automatiquement le JWT à chaque requête HTTP
 * ⚠️ N'injecte PAS AuthService pour éviter la dépendance circulaire
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  
  // ✅ Accéder directement au localStorage pour éviter la dépendance circulaire
  const token = localStorage.getItem(environment.tokenKey);

  // ✅ Si pas de token, ou si c'est une requête d'authentification, ne rien faire
  if (!token || 
      req.url.includes('/auth/login') || 
      req.url.includes('/auth/register') ||
      req.url.includes('/auth/refresh')) {
    return next(req);
  }

  // ✅ Cloner la requête et ajouter le header Authorization
  const clonedRequest = req.clone({
    setHeaders: {
      Authorization: `Bearer ${token}`
    }
  });

  console.log('🔐 Token ajouté à la requête:', req.url);

  // ✅ Gérer les erreurs 401 (token expiré)
  return next(clonedRequest).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401) {
        console.warn('⚠️ Token expiré ou invalide, déconnexion...');
        // ✅ Nettoyer les tokens directement
        localStorage.removeItem(environment.tokenKey);
        localStorage.removeItem(environment.refreshTokenKey);
        router.navigate(['/login'], {
          queryParams: { expired: 'true' }
        });
      }
      return throwError(() => error);
    })
  );
};