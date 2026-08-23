import { inject, Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthenticatedUser, LoginRequest, LoginResponse } from '../models/auth.models';

export const ACCESS_TOKEN_KEY = 'supportflow_access_token';
export const USER_KEY = 'supportflow_user';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly user = signal<AuthenticatedUser | null>(this.restoreUser());
  login(email: string, password: string) {
    const body: LoginRequest = { email, password };
    return this.http.post<LoginResponse>(`${environment.apiBaseUrl}/api/auth/login`, body).pipe(tap(response => {
      localStorage.setItem(ACCESS_TOKEN_KEY, response.accessToken);
      localStorage.setItem(USER_KEY, JSON.stringify(response.user));
      this.user.set(response.user);
    }));
  }
  logout(): void { localStorage.removeItem(ACCESS_TOKEN_KEY); localStorage.removeItem(USER_KEY); this.user.set(null); }
  getAccessToken(): string | null { return localStorage.getItem(ACCESS_TOKEN_KEY); }
  getCurrentUser(): AuthenticatedUser | null { return this.user(); }
  isAuthenticated(): boolean { return !!this.getAccessToken() && !!this.user(); }
  private restoreUser(): AuthenticatedUser | null {
    const stored = localStorage.getItem(USER_KEY); if (!stored) return null;
    try { return JSON.parse(stored) as AuthenticatedUser; } catch { localStorage.removeItem(USER_KEY); return null; }
  }
}
