import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AuthService } from '../auth/auth.service';
import { jwtInterceptor } from './jwt.interceptor';
import { environment } from '../../../environments/environment';

describe('jwtInterceptor', () => {
  it('adds the Bearer token to backend API requests', () => {
    TestBed.configureTestingModule({providers:[provideHttpClient(withInterceptors([jwtInterceptor])),provideHttpClientTesting(),{provide:AuthService,useValue:{getAccessToken:()=> 'jwt-token'}}]});
    TestBed.inject(HttpClient).get(`${environment.apiBaseUrl}/api/tickets`).subscribe();
    const request=TestBed.inject(HttpTestingController).expectOne(`${environment.apiBaseUrl}/api/tickets`);
    expect(request.request.headers.get('Authorization')).toBe('Bearer jwt-token');request.flush([]);
  });
  it('adds the Bearer token to the support-agents request',()=>{
    TestBed.configureTestingModule({providers:[provideHttpClient(withInterceptors([jwtInterceptor])),provideHttpClientTesting(),{provide:AuthService,useValue:{getAccessToken:()=> 'nicolas-token'}}]});
    TestBed.inject(HttpClient).get(`${environment.apiBaseUrl}/api/users/support-agents`).subscribe();
    const request=TestBed.inject(HttpTestingController).expectOne(`${environment.apiBaseUrl}/api/users/support-agents`);
    expect(request.request.headers.get('Authorization')).toBe('Bearer nicolas-token');request.flush([]);
  });
});
