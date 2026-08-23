import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { environment } from '../../../environments/environment';
import { TicketHistoryEntry } from '../models/ticket-history.models';

@Injectable({ providedIn: 'root' })
export class TicketHistoryService {
  private readonly http = inject(HttpClient);

  getHistory(ticketId: string) {
    return this.http.get<TicketHistoryEntry[]>(`${environment.apiBaseUrl}/api/tickets/${ticketId}/history`);
  }
}
