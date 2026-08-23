import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../environments/environment';
import { SlaSummary } from '../models/sla.models';
import { SlaService } from './sla.service';

describe('SlaService', () => {
  it('gets the ticket SLA from the expected endpoint', () => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    const service = TestBed.inject(SlaService);
    const http = TestBed.inject(HttpTestingController);
    const summary = { responseStatus: 'ON_TIME', resolutionStatus: 'AT_RISK' } as SlaSummary;
    let response: SlaSummary | undefined;
    service.getTicketSla('ticket-1').subscribe(value => response = value);
    const request = http.expectOne(`${environment.apiBaseUrl}/api/tickets/ticket-1/sla`);
    expect(request.request.method).toBe('GET');
    request.flush(summary);
    expect(response).toEqual(summary);
    http.verify();
  });
});
