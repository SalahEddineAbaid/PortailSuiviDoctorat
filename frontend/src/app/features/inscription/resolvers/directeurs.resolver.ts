import { inject } from '@angular/core';
import { ResolveFn } from '@angular/router';
import { catchError, of } from 'rxjs';
import { UserService } from '../../../core/services/user.service';
import { UserResponse } from '../../../core/models/user.model';

/**
 * Resolver pour précharger la liste des directeurs de thèse
 */
export const directeursResolver: ResolveFn<UserResponse[]> = () => {
  const userService = inject(UserService);

  return userService.getDirecteurs().pipe(
    catchError(error => {
      console.error('DirecteursResolver: Error loading directeurs', error);
      return of([]);
    })
  );
};
