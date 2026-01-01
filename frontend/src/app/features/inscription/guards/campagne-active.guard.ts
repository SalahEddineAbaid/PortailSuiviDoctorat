import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { map, catchError } from 'rxjs/operators';
import { of } from 'rxjs';
import { CampagneService } from '../../../core/services/campagne.service';

/**
 * Guard pour vérifier qu'une campagne active existe
 * avant de permettre la création d'une nouvelle inscription
 */
export const campagneActiveGuard: CanActivateFn = () => {
  const campagneService = inject(CampagneService);
  const router = inject(Router);

  return campagneService.hasActiveCampagne().pipe(
    map(hasActive => {
      if (hasActive) {
        return true;
      }
      
      console.warn('CampagneActiveGuard: No active campaign found');
      
      // Rediriger vers le dashboard avec un message
      router.navigate(['/inscription'], {
        queryParams: { 
          error: 'no-active-campaign',
          message: 'Aucune campagne d\'inscription n\'est actuellement active'
        }
      });
      
      return false;
    }),
    catchError(error => {
      console.error('CampagneActiveGuard: Error checking active campaign', error);
      router.navigate(['/inscription'], {
        queryParams: { 
          error: 'campaign-check-failed',
          message: 'Impossible de vérifier les campagnes actives'
        }
      });
      return of(false);
    })
  );
};

/**
 * Guard pour vérifier qu'une campagne de réinscription active existe
 */
export const campagneActiveReinscriptionGuard: CanActivateFn = () => {
  const campagneService = inject(CampagneService);
  const router = inject(Router);

  return campagneService.hasActiveCampagneReinscription().pipe(
    map(hasActive => {
      if (hasActive) {
        return true;
      }
      
      console.warn('CampagneActiveReinscriptionGuard: No active reinscription campaign found');
      
      router.navigate(['/inscription'], {
        queryParams: { 
          error: 'no-active-reinscription-campaign',
          message: 'Aucune campagne de réinscription n\'est actuellement active'
        }
      });
      
      return false;
    }),
    catchError(error => {
      console.error('CampagneActiveReinscriptionGuard: Error checking active reinscription campaign', error);
      router.navigate(['/inscription'], {
        queryParams: { 
          error: 'campaign-check-failed',
          message: 'Impossible de vérifier les campagnes actives'
        }
      });
      return of(false);
    })
  );
};
