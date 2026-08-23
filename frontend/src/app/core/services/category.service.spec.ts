import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../environments/environment';
import { CategoryService } from './category.service';

describe('CategoryService',()=>{it('loads categories without adding identity data',()=>{TestBed.configureTestingModule({providers:[provideHttpClient(),provideHttpClientTesting()]});TestBed.inject(CategoryService).getCategories().subscribe();const request=TestBed.inject(HttpTestingController).expectOne(`${environment.apiBaseUrl}/api/categories`);expect(request.request.method).toBe('GET');request.flush([]);});});
