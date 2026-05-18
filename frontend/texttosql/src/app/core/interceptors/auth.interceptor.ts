import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, filter, take } from 'rxjs/operators';
import { throwError, BehaviorSubject, from } from 'rxjs';
import { AuthService } from '../auth.service';
import { environment } from '../../../environments/environment';

const PUBLIC_PATHS: string[] = environment.PUBLIC_PATHS;

let isRefreshing = false;
const refreshTokenSubject: BehaviorSubject<string | null> = new BehaviorSubject<string | null>(null);

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService: AuthService = inject(AuthService);
  const isPublic: boolean = PUBLIC_PATHS.some(path => req.url.includes(path));

  const token: string | null = localStorage.getItem('token');
  let request = req.clone({ withCredentials: true });
  if (token) {
    request = request.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
  }

  return next(request).pipe(
    catchError((error: HttpErrorResponse) => {
      // If it's a public path, we don't try to refresh (e.g., login failure)
      if (isPublic) {
        return throwError(() => error);
      }

      if ((error.status === 401 || error.status === 403) && token) {
        // If the token in localStorage has already changed, it means another request 
        // has already refreshed it. We just retry with the new token.
        const currentToken = localStorage.getItem('token');
        if (currentToken && currentToken !== token) {
          return next(req.clone({ setHeaders: { Authorization: `Bearer ${currentToken}` }, withCredentials: true }));
        }

        if (!isRefreshing) {
          isRefreshing = true;
          refreshTokenSubject.next(null);

          return from(authService.doRefreshToken()).pipe(
            switchMap((newToken: string | null) => {
              isRefreshing = false;
              // Only retry if we got a DIFFERENT token to avoid infinite loops
              if (newToken && newToken !== token) {
                refreshTokenSubject.next(newToken);
                return next(req.clone({ setHeaders: { Authorization: `Bearer ${newToken}` }, withCredentials: true }));
              }
              return throwError(() => error);
            }),
            catchError((err) => {
              isRefreshing = false;
              return throwError(() => err);
            })
          );
        } else {
          return refreshTokenSubject.pipe(
            filter(t => t !== null),
            take(1),
            switchMap(jwt => {
              return next(req.clone({ setHeaders: { Authorization: `Bearer ${jwt}` }, withCredentials: true }));
            })
          );
        }
      }
      return throwError(() => error);
    })
  );
};
