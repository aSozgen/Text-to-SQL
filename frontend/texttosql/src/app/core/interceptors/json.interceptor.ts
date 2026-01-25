import { HttpInterceptorFn, HttpResponse } from '@angular/common/http';
import { map, switchMap } from 'rxjs/operators';
import { from, of } from 'rxjs';

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
    })
  );
};
