import { inject } from '@angular/core';
import { ResolveFn, ActivatedRouteSnapshot } from '@angular/router';
import { catchError, of } from 'rxjs';
import { CampagneService } from '../../../core/services/campagne.service';
import { CampagneResponse } from '../../../core/models/campagne.model';

/**
 * Resolver pour précharger la campagne active
 */
export const campagneActiveResolver: ResolveFn<CampagneResponse | null> = () => {
  const campagneService = inject(CampagneService);

  return campagneService.getCampagneActive().pipe(
    catchError(error => {
      console.error('CampagneActiveResolver: Error loading active campaign', error);
      return of(null);
    })
  );
};

/**
 * Resolver pour précharger une campagne spécifique
 */
export const campagneResolver: ResolveFn<CampagneResponse | null> = (
  route: ActivatedRouteSnapshot
) => {
  const campagneService = inject(CampagneService);

  const id = route.paramMap.get('id');

  if (!id) {
    console.error('CampagneResolver: No campagne ID provided');
    return of(null);
  }

  return campagneService.getCampagne(+id).pipe(
    catchError(error => {
      console.error('CampagneResolver: Error loading campagne', error);
      return of(null);
    })
  );
};

/**
 * Resolver pour précharger toutes les campagnes
 */
export const campagnesResolver: ResolveFn<CampagneResponse[]> = () => {
  const campagneService = inject(CampagneService);

  return campagneService.getAllCampagnes().pipe(
    catchError(error => {
      console.error('CampagnesResolver: Error loading campagnes', error);
      return of([]);
    })
  );
};

/**
 * Resolver pour précharger les campagnes actives
 */
export const campagnesActivesResolver: ResolveFn<CampagneResponse[]> = () => {
  const campagneService = inject(CampagneService);

  return campagneService.getCampagnesActives().pipe(
    catchError(error => {
      console.error('CampagnesActivesResolver: Error loading active campagnes', error);
      return of([]);
    })
  );
};
