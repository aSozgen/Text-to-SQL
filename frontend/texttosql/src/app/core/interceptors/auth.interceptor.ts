import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { tap } from 'rxjs/operators';
import { AuthService } from '../auth.service';
import {environment} from '../../../environments/environment';

const PUBLIC_PATHS: string[] = environment.PUBLIC_PATHS;

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService: AuthService = inject(AuthService);
  const isPublic: boolean = PUBLIC_PATHS.some(path => req.url.includes(path));

  if (isPublic) {
    return next(req);
  }

  const token : string | null = localStorage.getItem('token');
  const request = token
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  return next(request).pipe(
    tap({
      error: (err) => {
        if (err.status === 401 && token) {
          authService.logout();
        }
      }
    })
  );
};
