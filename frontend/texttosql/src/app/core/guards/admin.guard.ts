import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../auth.service';

export const adminGuard: CanActivateFn = async () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  await authService.initPromise;

  const user = authService.currentUser();
  if (user && user.role === 'ADMIN') {
    return true;
  }
  
  return router.createUrlTree(['/home']);
};
