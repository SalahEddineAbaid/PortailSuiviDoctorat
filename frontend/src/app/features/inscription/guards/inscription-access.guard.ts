import { inject } from '@angular/core';
import { Router, ActivatedRouteSnapshot, CanActivateFn } from '@angular/router';
import { map, catchError } from 'rxjs/operators';
import { of } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { InscriptionService } from '../../../core/services/inscription.service';
import { RoleName } from '../../../core/models/role.model';

/**
 * Guard pour vérifier l'accès à une inscription spécifique
 * 
 * Règles d'accès:
 * - DOCTORANT: Peut accéder uniquement à ses propres inscriptions
 * - DIRECTEUR: Peut accéder aux inscriptions de ses doctorants
 * - ADMIN: Peut accéder à toutes les inscriptions
 */
export const inscriptionAccessGuard: CanActivateFn = (route: ActivatedRouteSnapshot) => {
  const authService = inject(AuthService);
  const inscriptionService = inject(InscriptionService);
  const router = inject(Router);

  const inscriptionId = route.paramMap.get('id');
  
  if (!inscriptionId) {
    console.error('InscriptionAccessGuard: No inscription ID provided');
    router.navigate(['/inscription']);
    return false;
  }

  const currentUser = authService.getCurrentUser();
  
  if (!currentUser) {
    console.error('InscriptionAccessGuard: No current user');
    router.navigate(['/auth/login']);
    return false;
  }

  const userRole = currentUser.role?.name;
  const userId = currentUser.id;

  // Admin a accès à tout
  if (userRole === RoleName.ADMIN) {
    return true;
  }

  // Vérifier l'accès pour les autres rôles
  return inscriptionService.getInscription(+inscriptionId).pipe(
    map(inscription => {
      // Doctorant: Vérifier que c'est sa propre inscription
      if (userRole === RoleName.DOCTORANT) {
        if (inscription.doctorantId === userId) {
          return true;
        }
        console.warn(`InscriptionAccessGuard: Doctorant ${userId} trying to access inscription ${inscriptionId} of doctorant ${inscription.doctorantId}`);
        router.navigate(['/inscription']);
        return false;
      }

      // Directeur: Vérifier que c'est l'inscription d'un de ses doctorants
      if (userRole === RoleName.DIRECTEUR) {
        if (inscription.directeurTheseId === userId) {
          return true;
        }
        console.warn(`InscriptionAccessGuard: Directeur ${userId} trying to access inscription ${inscriptionId} of another directeur`);
        router.navigate(['/inscription']);
        return false;
      }

      // Rôle non reconnu
      console.error(`InscriptionAccessGuard: Unknown role ${userRole}`);
      router.navigate(['/']);
      return false;
    }),
    catchError(error => {
      console.error('InscriptionAccessGuard: Error fetching inscription', error);
      router.navigate(['/inscription']);
      return of(false);
    })
  );
};
