import { inject } from '@angular/core';
import { ResolveFn, Router } from '@angular/router';
import { catchError, of } from 'rxjs';
import { DashboardService } from '../services/dashboard.service';
import { AuthService } from '../../../core/services/auth.service';
import { AdminDashboard } from '../models/dashboard.model';

/**
 * 🛠️ Resolver pour précharger les données du dashboard admin
 */
export const adminDashboardResolver: ResolveFn<AdminDashboard | null> = (route, state) => {
  const dashboardService = inject(DashboardService);
  const authService = inject(AuthService);
  const router = inject(Router);

  console.log('🔄 [RESOLVER] Préchargement dashboard admin...');

  // Vérifier le rôle
  if (!authService.hasRole('ROLE_ADMIN')) {
    console.error('❌ [RESOLVER] Utilisateur n\'a pas le rôle ADMIN');
    router.navigate(['/unauthorized']);
    return of(null);
  }

  return dashboardService.getAdminDashboard().pipe(
    catchError(error => {
      console.error('❌ [RESOLVER] Erreur chargement dashboard admin:', error);
      return of(null);
    })
  );
};
