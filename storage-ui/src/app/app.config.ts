import { ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import {
  provideKeycloak,
  createInterceptorCondition,
  IncludeBearerTokenCondition,
  INCLUDE_BEARER_TOKEN_INTERCEPTOR_CONFIG,
  includeBearerTokenInterceptor,
  withAutoRefreshToken,
  AutoRefreshTokenService,
  UserActivityService,
} from 'keycloak-angular';

import { routes } from './app.routes';
import { environment } from '../environments/environment';

const apiBearerCondition = createInterceptorCondition<IncludeBearerTokenCondition>({
  urlPattern: new RegExp(`^${environment.apiUrl}(/.*)?$`, 'i'),
  bearerPrefix: 'Bearer',
});

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideKeycloak({
      config: {
        url: environment.keycloakUrl,
        realm: environment.keycloakRealm,
        clientId: environment.keycloakClientId,
      },
      initOptions: {
        onLoad: 'login-required',
      },
      features: [
        withAutoRefreshToken({ onInactivityTimeout: 'logout', sessionTimeout: 300_000 }),
      ],
      providers: [AutoRefreshTokenService, UserActivityService],
    }),
    { provide: INCLUDE_BEARER_TOKEN_INTERCEPTOR_CONFIG, useValue: [apiBearerCondition] },
    provideHttpClient(withInterceptors([includeBearerTokenInterceptor])),
  ],
};
