import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { of, Subject, throwError } from 'rxjs';
import { SlaStatus, SlaSummary } from '../../../core/models/sla.models';
import { SlaService } from '../../../core/services/sla.service';
import { formatRemainingSeconds, TicketSlaComponent } from './ticket-sla.component';

describe('TicketSlaComponent', () => {
  const summary: SlaSummary = {
    responseDueAt: '2026-08-21T15:37:59Z', resolutionDueAt: '2026-08-21T19:07:59Z',
    firstResponseAt: '2026-08-21T15:11:32Z', resolvedAt: null,
    responseStatus: 'COMPLETED', resolutionStatus: 'ON_TIME',
    responseRemainingSeconds: 0, resolutionRemainingSeconds: 12000
  };
  const service = { getTicketSla: vi.fn() };

  async function render(result = of(summary)) {
    service.getTicketSla.mockReturnValue(result);
    await TestBed.configureTestingModule({ imports: [TicketSlaComponent], providers: [{ provide: SlaService, useValue: service }] }).compileComponents();
    const fixture = TestBed.createComponent(TicketSlaComponent);
    fixture.componentRef.setInput('ticketId', 'ticket-1'); fixture.detectChanges();
    return fixture;
  }

  beforeEach(() => { TestBed.resetTestingModule(); service.getTicketSla.mockReset(); });

  it('loads SLA with the ticket id', async () => { await render(); expect(service.getTicketSla).toHaveBeenCalledWith('ticket-1'); });
  it.each([
    ['ON_TIME', 'Dans les délais'], ['AT_RISK', 'À risque'], ['BREACHED', 'SLA dépassé'], ['COMPLETED', 'Terminé']
  ] as [SlaStatus, string][])('translates %s into French', async (status, label) => {
    const fixture = await render(of({ ...summary, responseStatus: status }));
    expect(fixture.nativeElement.textContent).toContain(label);
    expect(fixture.nativeElement.querySelector(`[data-status="${status}"]`)).not.toBeNull();
  });
  it.each([[45, '45 s'], [300, '5 min'], [3900, '1 h 5 min'], [10800, '3 h'], [90000, '1 j 1 h']] as [number, string][])('formats %i seconds', (seconds, expected) => { expect(formatRemainingSeconds(seconds)).toBe(expected); });
  it('shows Terminé instead of zero seconds for COMPLETED', async () => { const text = (await render()).nativeElement.textContent; expect(text).toContain('Terminé'); });
  it('shows Délai dépassé for a breached zero remainder', async () => { const fixture = await render(of({ ...summary, resolutionStatus: 'BREACHED', resolutionRemainingSeconds: 0 })); expect(fixture.nativeElement.textContent).toContain('Délai dépassé'); });
  it('handles a null first response', async () => { const fixture = await render(of({ ...summary, firstResponseAt: null })); expect(fixture.nativeElement.textContent).toContain('Non enregistrée'); });
  it('handles a null resolution date', async () => { expect((await render()).nativeElement.textContent).toContain('Non résolu'); });
  it('displays the resolution date when present', async () => { const fixture = await render(of({ ...summary, resolvedAt: '2026-08-21T18:00:00Z' })); expect(fixture.nativeElement.textContent).not.toContain('Non résolu'); expect(fixture.nativeElement.textContent).toContain('21/08/2026'); });
  it('shows its loading state', async () => { const fixture = await render(new Subject<SlaSummary>()); expect(fixture.nativeElement.textContent).toContain('Chargement du SLA'); });
  it.each([[403, "Vous n'avez pas accès aux informations SLA."], [404, 'Informations SLA introuvables.'], [500, 'Impossible de charger les informations SLA.']] as [number, string][])('handles HTTP %i locally', async (status, message) => {
    const fixture = await render(throwError(() => new HttpErrorResponse({ status })));
    expect(fixture.nativeElement.textContent).toContain(message);
  });
  it('retries only the SLA request', async () => {
    const fixture = await render(throwError(() => new Error('network'))); service.getTicketSla.mockReturnValue(of(summary));
    (fixture.nativeElement.querySelector('button') as HTMLButtonElement).click(); fixture.detectChanges();
    expect(service.getTicketSla).toHaveBeenCalledTimes(2); expect(fixture.nativeElement.textContent).toContain('Dans les délais');
  });
});
