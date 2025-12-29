import { ApplicationConfig, provideZoneChangeDetection, ErrorHandler } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAnimations } from '@angular/platform-browser/animations';
import { routes } from './app.routes';
import { authInterceptor } from './core/interceptors/auth.interceptor';
import { errorInterceptor } from './core/interceptors/error.interceptor';
import { GlobalErrorHandler } from './core/handlers/global-error.handler';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideAnimations(),  // ← Ajout des animations Angular Material
    provideHttpClient(
      withInterceptors([authInterceptor, errorInterceptor])  // ← Ajout des interceptors
    ),
    { provide: ErrorHandler, useClass: GlobalErrorHandler }  // ← Ajout du GlobalErrorHandler
  ]
};