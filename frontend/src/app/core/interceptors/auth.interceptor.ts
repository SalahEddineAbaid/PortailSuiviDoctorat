import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

/**
 * 🔐 Interceptor qui ajoute automatiquement le JWT à chaque requête HTTP
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  
  // ✅ Utiliser getToken() au lieu de getAccessToken()
  const token = authService.getToken();

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
        authService.logout();
        router.navigate(['/login'], {
          queryParams: { expired: 'true' }
        });
      }
      return throwError(() => error);
    })
  );
};