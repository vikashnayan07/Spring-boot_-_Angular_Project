import { ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideAnimations } from '@angular/platform-browser/animations';
import { routes } from './app.routes';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { authInterceptor } from './core/interceptors/auth.interceptors';



export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    // 👉 1. Keep the one WITH the interceptor
    provideHttpClient(withInterceptors([authInterceptor])),
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideAnimations()
    // 👉 2. The duplicate provideHttpClient() at the bottom has been DELETED
  ]
};
