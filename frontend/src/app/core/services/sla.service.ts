import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { environment } from '../../../environments/environment';
import { SlaSummary } from '../models/sla.models';

@Injectable({ providedIn: 'root' })
export class SlaService {
  private readonly http = inject(HttpClient);

  getTicketSla(ticketId: string) {
    return this.http.get<SlaSummary>(`${environment.apiBaseUrl}/api/tickets/${ticketId}/sla`);
  }
}
