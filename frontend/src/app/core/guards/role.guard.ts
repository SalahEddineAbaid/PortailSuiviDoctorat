import { inject } from '@angular/core';
import { Router, CanActivateFn, ActivatedRouteSnapshot } from '@angular/router';
import { AuthService } from '../services/auth.service';

/**
 * 🎭 Guard qui vérifie si l'utilisateur a le rôle requis
 * 
 * Utilisation dans les routes :
 * ```typescript
 * {
 *   path: 'admin',
 *   component: AdminComponent,
 *   canActivate: [authGuard, roleGuard],
 *   data: { role: 'ROLE_ADMIN' }
 * }
 * ```
 */
export const roleGuard: CanActivateFn = (route: ActivatedRouteSnapshot) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  // Récupérer le rôle requis depuis les données de la route
  const requiredRole = route.data['role'] as string;

  if (!requiredRole) {
    console.error('❌ RoleGuard : Aucun rôle spécifié dans les données de la route');
    return true; // Laisser passer si pas de rôle spécifié
  }

  // Vérifier si l'utilisateur a le rôle requis
  if (authService.hasRole(requiredRole)) {
    console.log(`✅ RoleGuard : Utilisateur a le rôle ${requiredRole}`);
    return true;
  }

  // Si l'utilisateur n'a pas le rôle, rediriger vers une page d'erreur
  console.warn(`⚠️ RoleGuard : Utilisateur n'a pas le rôle ${requiredRole}`);
  router.navigate(['/unauthorized']);
  return false;
};
