import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../environments/environment';
import { CommentService } from './comment.service';

describe('CommentService', () => {
  let service: CommentService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(CommentService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('gets ticket comments', () => {
    service.getComments('ticket-1').subscribe();
    const request = http.expectOne(`${environment.apiBaseUrl}/api/tickets/ticket-1/comments`);
    expect(request.request.method).toBe('GET');
    request.flush([]);
  });

  it('posts a public comment', () => {
    service.addComment('ticket-1', { content: 'Information publique', internal: false }).subscribe();
    const request = http.expectOne(`${environment.apiBaseUrl}/api/tickets/ticket-1/comments`);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ content: 'Information publique', internal: false });
    request.flush({});
  });

  it('posts an internal note', () => {
    service.addComment('ticket-1', { content: 'Diagnostic support', internal: true }).subscribe();
    const request = http.expectOne(`${environment.apiBaseUrl}/api/tickets/ticket-1/comments`);
    expect(request.request.body).toEqual({ content: 'Diagnostic support', internal: true });
    request.flush({});
  });
});
