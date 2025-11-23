import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthService } from '../services/auth.service';

/**
 * Guard qui vérifie si l'utilisateur est authentifié
 * Si non connecté → redirection vers /login
 */
export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  // Vérifier si l'utilisateur est authentifié
  if (authService.isAuthenticated()) {
    console.log('✅ AuthGuard : Utilisateur authentifié');
    return true;
  }

  // Si non authentifié, rediriger vers login
  console.warn('⚠️ AuthGuard : Non authentifié, redirection vers /login');
  router.navigate(['/login'], {
    queryParams: { returnUrl: state.url }  // Sauvegarder l'URL demandée
  });
  return false;
};