import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

// ==========================================
// PUBLIC IMPORTS
// ==========================================
import { LandingComponent } from './features/landing/landing/landing.component';
import { LoginComponent } from './features/auth/login/login.component';
import { SetupAccountComponent } from './features/auth/setup-account/setup-account.component';
import { ForgotPasswordComponent } from './features/auth/forgot-password/forgot-password.component';
import { FaultLogComponent } from './features/fault-log/fault-log/fault-log.component';

// ==========================================
// ROUTE CONFIGURATION
// ==========================================
export const routes: Routes = [
  // 1. PUBLIC ROUTES
  { path: '', component: LandingComponent },
  { path: 'landing', component: LandingComponent },
  { path: 'login', component: LoginComponent },
  { path: 'setup-account', component: SetupAccountComponent },
  { path: 'forgot-password', component: ForgotPasswordComponent },

  // 2. SHARED FEATURE ROUTES
  { path: 'fault-log', component: FaultLogComponent, canActivate: [authGuard] },
  { path: 'add-fault', redirectTo: 'operator/log-error' },

  // 3. ADMIN SECURE ROUTES (Lazy Loaded)
  {
    path: 'admin',
    loadChildren: () =>
      import('./features/admin/admin.routes').then((m) => m.adminRoutes),
  },

  // 4. OPERATOR SECURE ROUTES (Lazy Loaded)
  {
    path: 'operator',
    loadChildren: () =>
      import('./features/operator/operator.routes').then(
        (m) => m.operatorRoutes,
      ),
  },

  // 5. ENGINEER SECURE ROUTES (Lazy Loaded)
  {
    path: 'engineer',
    loadChildren: () =>
      import('./features/engineer/engineer.routes').then(
        (m) => m.engineerRoutes,
      ),
  },

  // 6. CATCH-ALL ROUTE (Prevents blank screens if a user types a bad URL)
  { path: '**', redirectTo: '' },
];
