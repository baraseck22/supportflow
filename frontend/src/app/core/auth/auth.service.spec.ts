import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AuthService, ACCESS_TOKEN_KEY, USER_KEY } from './auth.service';
import { environment } from '../../../environments/environment';

describe('AuthService', () => {
  let service: AuthService; let http: HttpTestingController;
  const user = { id:'1', firstName:'Alice', lastName:'Martin', email:'alice.user@supportflow.local', role:'USER' as const };
  beforeEach(() => { localStorage.clear(); TestBed.configureTestingModule({providers:[provideHttpClient(),provideHttpClientTesting()]}); service=TestBed.inject(AuthService);http=TestBed.inject(HttpTestingController); });
  afterEach(() => http.verify());
  it('saves token and user after login', () => {
    service.login(user.email,'secret').subscribe();
    http.expectOne(`${environment.apiBaseUrl}/api/auth/login`).flush({accessToken:'jwt-token',tokenType:'Bearer',expiresIn:3600,user});
    expect(localStorage.getItem(ACCESS_TOKEN_KEY)).toBe('jwt-token'); expect(service.getCurrentUser()).toEqual(user);
  });
  it('removes token and user on logout', () => {
    localStorage.setItem(ACCESS_TOKEN_KEY,'token');localStorage.setItem(USER_KEY,JSON.stringify(user));service.logout();
    expect(localStorage.getItem(ACCESS_TOKEN_KEY)).toBeNull();expect(localStorage.getItem(USER_KEY)).toBeNull();
  });
});
