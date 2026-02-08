import { ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import {PreloadAllModules, provideRouter, withPreloading, withViewTransitions} from '@angular/router';

import { routes } from './app.routes';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideApiConfiguration } from './api/api-configuration';
import {authInterceptor} from './core/interceptors/auth.interceptor';
import {blobToJsonInterceptor} from './core/interceptors/json.interceptor';
import {environment} from '../environments/environment';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes,
      withPreloading(PreloadAllModules),
      withViewTransitions()
    ),
    provideHttpClient(withInterceptors([
      authInterceptor,
      blobToJsonInterceptor
    ])),
    provideApiConfiguration(environment.apiBaseUrl)
  ]
};
