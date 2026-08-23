import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../environments/environment';
import { TicketHistoryEntry } from '../models/ticket-history.models';
import { TicketHistoryService } from './ticket-history.service';

describe('TicketHistoryService', () => {
  let service: TicketHistoryService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(TicketHistoryService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('gets and deserializes ticket history from the expected endpoint', () => {
    const history: TicketHistoryEntry[] = [{
      id: 'history-1', fieldName: 'status', oldValue: 'NEW', newValue: 'IN_PROGRESS',
      changedBy: { id: 'support-1', firstName: 'Nicolas', lastName: 'Support', email: 'nicolas@supportflow.local', role: 'SUPPORT_N1' },
      createdAt: '2026-08-21T14:17:15Z'
    }];
    let response: TicketHistoryEntry[] | undefined;
    service.getHistory('ticket-1').subscribe(value => response = value);
    const request = http.expectOne(`${environment.apiBaseUrl}/api/tickets/ticket-1/history`);
    expect(request.request.method).toBe('GET');
    request.flush(history);
    expect(response).toEqual(history);
  });
});
