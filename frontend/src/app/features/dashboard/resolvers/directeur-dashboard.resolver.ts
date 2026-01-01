import { inject } from '@angular/core';
import { ResolveFn, Router } from '@angular/router';
import { catchError, of } from 'rxjs';
import { DashboardService } from '../services/dashboard.service';
import { AuthService } from '../../../core/services/auth.service';
import { DirecteurDashboard } from '../models/dashboard.model';

/**
 * 👨‍🏫 Resolver pour précharger les données du dashboard directeur
 */
export const directeurDashboardResolver: ResolveFn<DirecteurDashboard | null> = (route, state) => {
  const dashboardService = inject(DashboardService);
  const authService = inject(AuthService);
  const router = inject(Router);

  console.log('🔄 [RESOLVER] Préchargement dashboard directeur...');

  // Récupérer l'utilisateur connecté
  const user = authService.currentUser$.value;

  if (!user || !user.id) {
    console.error('❌ [RESOLVER] Utilisateur non connecté');
    router.navigate(['/login']);
    return of(null);
  }

  // Vérifier le rôle
  if (!authService.hasRole('ROLE_DIRECTEUR')) {
    console.error('❌ [RESOLVER] Utilisateur n\'a pas le rôle DIRECTEUR');
    router.navigate(['/unauthorized']);
    return of(null);
  }

  return dashboardService.getDirecteurDashboard(user.id).pipe(
    catchError(error => {
      console.error('❌ [RESOLVER] Erreur chargement dashboard directeur:', error);
      return of(null);
    })
  );
};
