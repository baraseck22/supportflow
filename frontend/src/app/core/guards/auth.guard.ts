import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../auth/auth.service';
export const authGuard: CanActivateFn = () => inject(AuthService).isAuthenticated() ? true : inject(Router).createUrlTree(['/login']);
export const guestGuard: CanActivateFn = () => inject(AuthService).isAuthenticated() ? inject(Router).createUrlTree(['/app']) : true;
export const rootGuard: CanActivateFn = () => inject(Router).createUrlTree([inject(AuthService).isAuthenticated() ? '/app' : '/login']);
export const supportRoleGuard: CanActivateFn = () => {const role=inject(AuthService).getCurrentUser()?.role;return role&&role!=='USER'?true:inject(Router).createUrlTree(['/app/tickets']);};
