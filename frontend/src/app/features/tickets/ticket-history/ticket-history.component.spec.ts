import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { TicketHistoryEntry } from '../../../core/models/ticket-history.models';
import { TicketHistoryService } from '../../../core/services/ticket-history.service';
import { TicketHistoryComponent } from './ticket-history.component';

describe('TicketHistoryComponent', () => {
  const actor = { id: 'support-1', firstName: 'Nicolas', lastName: 'Support', email: 'nicolas@supportflow.local', role: 'SUPPORT_N1' as const };
  const entry = (fieldName: string, oldValue: string | null, newValue: string | null, id = fieldName): TicketHistoryEntry => ({
    id, fieldName, oldValue, newValue, changedBy: actor, createdAt: '2026-08-21T14:17:15Z'
  });
  const service = { getHistory: vi.fn() };

  async function render(result = of<TicketHistoryEntry[]>([])) {
    service.getHistory.mockReturnValue(result);
    await TestBed.configureTestingModule({
      imports: [TicketHistoryComponent],
      providers: [{ provide: TicketHistoryService, useValue: service }]
    }).compileComponents();
    const fixture = TestBed.createComponent(TicketHistoryComponent);
    fixture.componentRef.setInput('ticketId', 'ticket-1');
    fixture.detectChanges();
    return fixture;
  }

  beforeEach(() => { TestBed.resetTestingModule(); service.getHistory.mockReset(); });

  it('loads history for the ticket', async () => { await render(); expect(service.getHistory).toHaveBeenCalledWith('ticket-1'); });
  it('displays ticket creation', async () => { expect((await render(of([entry('ticket', null, 'INC-000082')]))).nativeElement.textContent).toContain('Ticket INC-000082 créé'); });
  it('translates a NEW to IN_PROGRESS transition', async () => { expect((await render(of([entry('status', 'NEW', 'IN_PROGRESS')]))).nativeElement.textContent).toContain('Statut modifié : Nouveau → En cours'); });
  it('displays assignment', async () => { expect((await render(of([entry('assignedTo', null, 'agent-id')]))).nativeElement.textContent).toContain('Ticket affecté'); });
  it('displays the first-response SLA event', async () => { expect((await render(of([entry('sla', null, 'FIRST_RESPONSE_RECORDED')]))).nativeElement.textContent).toContain('Première réponse SLA enregistrée'); });
  it('displays a public-comment event', async () => { expect((await render(of([entry('comment', null, 'PUBLIC_COMMENT_ADDED')]))).nativeElement.textContent).toContain('Commentaire public ajouté'); });
  it('displays an internal-note event when returned', async () => { expect((await render(of([entry('comment', null, 'INTERNAL_NOTE_ADDED')]))).nativeElement.textContent).toContain('Note interne ajoutée'); });
  it('translates the escalation reason',async()=>{expect((await render(of([entry('escalationReason',null,'Analyse N2 nécessaire')]))).nativeElement.textContent).toContain("Motif d'escalade : Analyse N2 nécessaire");});
  it('does not invent an internal event absent from the response', async () => { const text = (await render(of([entry('comment', null, 'PUBLIC_COMMENT_ADDED')]))).nativeElement.textContent; expect(text).not.toContain('Note interne ajoutée'); });
  it('displays the empty state only for an empty response', async () => { expect((await render()).nativeElement.textContent).toContain('Aucun événement enregistré.'); });
  it('displays an isolated loading error', async () => { expect((await render(throwError(() => new Error('network')))).nativeElement.textContent).toContain("Impossible de charger l'historique."); });
  it('retries only the history request', async () => {
    const fixture = await render(throwError(() => new Error('network')));
    service.getHistory.mockReturnValue(of([]));
    (fixture.nativeElement.querySelector('button') as HTMLButtonElement).click(); fixture.detectChanges();
    expect(service.getHistory).toHaveBeenCalledTimes(2);
    expect(fixture.nativeElement.textContent).toContain('Aucun événement enregistré.');
  });
});
