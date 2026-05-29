import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';

export const roleGuard: CanActivateFn = (route) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const requiredRoles = (route.data?.['roles'] as number[]) || [];
  const roleId = authService.getRoleId();

  if (!authService.isLoggedIn()) {
    router.navigate(['/login']);
    return false;
  }

  if (requiredRoles.length === 0 || requiredRoles.includes(roleId)) {
    return true;
  }

  router.navigate(['/login']);
  return false;
};
