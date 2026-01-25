import { ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideApiConfiguration } from './api/api-configuration';
import {authInterceptor} from './core/interceptors/auth.interceptor';
import {blobToJsonInterceptor} from './core/interceptors/json.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideHttpClient(withInterceptors([
      authInterceptor,
      blobToJsonInterceptor
    ])),
    provideApiConfiguration('http://localhost:8080')
  ]
};
