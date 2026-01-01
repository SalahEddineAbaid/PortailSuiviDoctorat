import { inject } from '@angular/core';
import { ResolveFn, Router } from '@angular/router';
import { catchError, of } from 'rxjs';
import { DashboardService } from '../services/dashboard.service';
import { AuthService } from '../../../core/services/auth.service';
import { DoctorantDashboard } from '../models/dashboard.model';

/**
 * 🎓 Resolver pour précharger les données du dashboard doctorant
 */
export const doctorantDashboardResolver: ResolveFn<DoctorantDashboard | null> = (route, state) => {
  const dashboardService = inject(DashboardService);
  const authService = inject(AuthService);
  const router = inject(Router);

  console.log('🔄 [RESOLVER] Préchargement dashboard doctorant...');

  // Récupérer l'utilisateur connecté via pipe
  let user: any = null;
  authService.currentUser$.subscribe(u => user = u).unsubscribe();

  if (!user || !user.id) {
    console.error('❌ [RESOLVER] Utilisateur non connecté');
    router.navigate(['/login']);
    return of(null);
  }

  // Vérifier le rôle
  if (!authService.hasRole('ROLE_DOCTORANT')) {
    console.error('❌ [RESOLVER] Utilisateur n\'a pas le rôle DOCTORANT');
    router.navigate(['/unauthorized']);
    return of(null);
  }

  return dashboardService.getDoctorantDashboard(user.id).pipe(
    catchError(error => {
      console.error('❌ [RESOLVER] Erreur chargement dashboard doctorant:', error);
      // Retourner un dashboard vide plutôt que de bloquer la navigation
      return of(null);
    })
  );
};
