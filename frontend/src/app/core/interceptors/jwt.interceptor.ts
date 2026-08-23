import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../auth/auth.service';
import { environment } from '../../../environments/environment';

export const jwtInterceptor: HttpInterceptorFn = (request, next) => {
  const token = inject(AuthService).getAccessToken();
  const isApi = request.url.startsWith(`${environment.apiBaseUrl}/api/`);
  const isLogin = request.url === `${environment.apiBaseUrl}/api/auth/login`;
  return next(token && isApi && !isLogin
    ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : request);
};
