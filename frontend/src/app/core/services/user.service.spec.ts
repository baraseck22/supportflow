import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../environments/environment';
import { UserService } from './user.service';

describe('UserService',()=>{it('loads support agents without managing Authorization itself',()=>{TestBed.configureTestingModule({providers:[provideHttpClient(),provideHttpClientTesting()]});TestBed.inject(UserService).getSupportAgents().subscribe();const request=TestBed.inject(HttpTestingController).expectOne(`${environment.apiBaseUrl}/api/users/support-agents`);expect(request.request.method).toBe('GET');expect(request.request.headers.has('Authorization')).toBe(false);request.flush([]);});});
