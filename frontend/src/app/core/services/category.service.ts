import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { environment } from '../../../environments/environment';
import { CategorySummary } from '../models/ticket.models';

@Injectable({ providedIn: 'root' })
export class CategoryService {
  private readonly http = inject(HttpClient);
  getCategories() { return this.http.get<CategorySummary[]>(`${environment.apiBaseUrl}/api/categories`); }
}
