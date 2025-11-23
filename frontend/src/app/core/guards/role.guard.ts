import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { RoleName } from '../models/role.model';


/**
 * Guard qui vérifie si l'utilisateur a le rôle requis
 * Si mauvais rôle → redirection vers son dashboard
 */
export const roleGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  // Récupérer le rôle requis depuis les données de la route
  const requiredRole = route.data['role'] as RoleName;

  if (!requiredRole) {
    console.error('❌ RoleGuard : Aucun rôle défini dans route.data');
    return true;
  }

  // Récupérer le rôle de l'utilisateur
  const userRole = authService.getUserRole();

  if (!userRole) {
    console.warn('⚠️ RoleGuard : Aucun rôle trouvé pour l\'utilisateur');
    router.navigate(['/login']);
    return false;
  }

  // ✅ Convertir RoleName en string pour comparer
  const requiredRoleString = `ROLE_${requiredRole}`;

  // Vérifier si l'utilisateur a le bon rôle
  if (userRole === requiredRoleString) {
    console.log(`✅ RoleGuard : Rôle ${requiredRoleString} autorisé`);
    return true;
  }

  // Si mauvais rôle, rediriger vers son propre dashboard
  console.warn(`⚠️ RoleGuard : Rôle ${userRole} non autorisé pour ${requiredRoleString}`);
  const correctRoute = authService.getDashboardRoute();
  router.navigate([correctRoute]);
  return false;
};