import { HttpInterceptorFn, HttpResponse, HttpErrorResponse } from '@angular/common/http';
import { map, switchMap, catchError } from 'rxjs/operators';
import { from, of, throwError } from 'rxjs';

export const blobToJsonInterceptor: HttpInterceptorFn = (req, next) => {
  return next(req).pipe(
    switchMap(event => {
      if (event instanceof HttpResponse) {
        if (event.body instanceof Blob && event.body.type.includes('application/json')) {
          return from(event.body.text()).pipe(
            map(jsonText => {
              try {
                const body = JSON.parse(jsonText);
                return event.clone({ body });
              } catch (e) {
                return event;
              }
            })
          );
        }
      }
      return of(event);
    }),

    catchError((error: HttpErrorResponse) => {
      if (error.error instanceof Blob) {
        return from(error.error.text()).pipe(
          switchMap(text => {
            let parsedError: any;
            try { parsedError = JSON.parse(text); }
            catch { parsedError = { message: text }; }

            return throwError(() => new HttpErrorResponse({
              error: parsedError,
              status: error.status,
              statusText: error.statusText,
              url: error.url ?? undefined,
              headers: error.headers
            }));
          })
        );
      }
      return throwError(() => error);
    })

  );
};
