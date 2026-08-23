import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { environment } from '../../../environments/environment';
import { AuthenticatedUser } from '../models/auth.models';

@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly http = inject(HttpClient);
  getSupportAgents() { return this.http.get<AuthenticatedUser[]>(`${environment.apiBaseUrl}/api/users/support-agents`); }
}
