import { inject } from '@angular/core';
import { ResolveFn, ActivatedRouteSnapshot, Router } from '@angular/router';
import { catchError, of } from 'rxjs';
import { InscriptionService } from '../../../core/services/inscription.service';
import { InscriptionResponse } from '../../../core/models/inscription.model';

/**
 * Resolver pour précharger les données d'une inscription
 */
export const inscriptionResolver: ResolveFn<InscriptionResponse | null> = (
  route: ActivatedRouteSnapshot
) => {
  const inscriptionService = inject(InscriptionService);
  const router = inject(Router);

  const id = route.paramMap.get('id');

  if (!id) {
    console.error('InscriptionResolver: No inscription ID provided');
    router.navigate(['/inscription']);
    return of(null);
  }

  return inscriptionService.getInscription(+id).pipe(
    catchError(error => {
      console.error('InscriptionResolver: Error loading inscription', error);
      router.navigate(['/inscription'], {
        queryParams: {
          error: 'inscription-not-found',
          message: 'Inscription non trouvée'
        }
      });
      return of(null);
    })
  );
};

/**
 * Resolver pour précharger les inscriptions d'un doctorant
 */
export const inscriptionsDoctorantResolver: ResolveFn<InscriptionResponse[]> = (
  route: ActivatedRouteSnapshot
) => {
  const inscriptionService = inject(InscriptionService);
  const router = inject(Router);

  const doctorantId = route.paramMap.get('doctorantId');

  if (!doctorantId) {
    console.error('InscriptionsDoctorantResolver: No doctorant ID provided');
    return of([]);
  }

  return inscriptionService.getInscriptionsDoctorant(+doctorantId).pipe(
    catchError(error => {
      console.error('InscriptionsDoctorantResolver: Error loading inscriptions', error);
      return of([]);
    })
  );
};
