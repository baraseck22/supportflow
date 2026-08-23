import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { environment } from '../../../environments/environment';
import { DashboardSummary } from '../models/dashboard.models';
@Injectable({providedIn:'root'})export class DashboardService{private readonly http=inject(HttpClient);getSummary(){return this.http.get<DashboardSummary>(`${environment.apiBaseUrl}/api/dashboard/summary`);}}
