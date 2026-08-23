import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, Router } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { CommentResponse } from '../../core/models/comment.models';
import { Role } from '../../core/models/auth.models';
import { Ticket } from '../../core/models/ticket.models';
import { CommentService } from '../../core/services/comment.service';
import { TicketService } from '../../core/services/ticket.service';
import { TicketHistoryService } from '../../core/services/ticket-history.service';
import { SlaService } from '../../core/services/sla.service';
import { SlaSummary } from '../../core/models/sla.models';
import { UserService } from '../../core/services/user.service';
import { TicketActionsComponent } from './ticket-actions/ticket-actions.component';
import { TicketDetailComponent } from './ticket-detail.component';

describe('TicketDetailComponent', () => {
  const ticket: Ticket = {
    id: 'ticket-1', ticketNumber: 'INC-000001', title: 'API de paiement indisponible',
    description: 'Toutes les tentatives de paiement échouent.', status: 'IN_PROGRESS', priority: 'CRITICAL',
    category: { id: 'cat-1', name: 'APPLICATION_ERROR', description: null },
    createdBy: { id: 'user-1', firstName: 'Alice', lastName: 'Martin', email: 'alice@supportflow.local', role: 'USER' },
    assignedTo: null, createdAt: '2026-08-21T10:00:00Z', updatedAt: '2026-08-21T10:30:00Z',
    resolvedAt: null, closedAt: null, firstResponseAt: null,
    responseDueAt: '2026-08-21T10:15:00Z', resolutionDueAt: '2026-08-21T12:00:00Z'
  };
  const publicComment: CommentResponse = {
    id: 'comment-1', content: 'Information publique', internal: false,
    author: { id: 'user-1', firstName: 'Alice', lastName: 'Martin', email: 'alice@supportflow.local', role: 'USER' },
    createdAt: '2026-08-21T11:00:00Z', updatedAt: '2026-08-21T11:00:00Z'
  };
  const internalComment: CommentResponse = {
    ...publicComment, id: 'comment-2', content: 'Diagnostic interne', internal: true,
    author: { id: 'support-1', firstName: 'Nicolas', lastName: 'Support', email: 'nicolas@supportflow.local', role: 'SUPPORT_N1' }
  };

  let role: Role = 'USER';
  const ticketService = { getTicketById: vi.fn() };
  const commentService = { getComments: vi.fn(), addComment: vi.fn() };
  const historyService = { getHistory: vi.fn() };
  const slaService = { getTicketSla: vi.fn() };
  const userService = { getSupportAgents: vi.fn() };
  const router = { navigate: vi.fn() };

  const slaSummary = { responseStatus: 'ON_TIME', resolutionStatus: 'ON_TIME' } as SlaSummary;
  async function render(ticketResult = of(ticket), commentsResult = of<CommentResponse[]>([]), historyResult = of([]), slaResult = of(slaSummary)) {
    ticketService.getTicketById.mockReturnValue(ticketResult);
    commentService.getComments.mockReturnValue(commentsResult);
    historyService.getHistory.mockReturnValue(historyResult);
    slaService.getTicketSla.mockReturnValue(slaResult);
    userService.getSupportAgents.mockReturnValue(of([]));
    await TestBed.configureTestingModule({
      imports: [TicketDetailComponent],
      providers: [
        { provide: TicketService, useValue: ticketService },
        { provide: CommentService, useValue: commentService },
        { provide: TicketHistoryService, useValue: historyService },
        { provide: SlaService, useValue: slaService },
        { provide: UserService, useValue: userService },
        { provide: AuthService, useValue: { getCurrentUser: () => ({ role }) } },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ id: 'ticket-1' }) } } },
        { provide: Router, useValue: router }
      ]
    }).compileComponents();
    const fixture = TestBed.createComponent(TicketDetailComponent);
    fixture.detectChanges();
    return fixture;
  }

  beforeEach(() => {
    TestBed.resetTestingModule();
    ticketService.getTicketById.mockReset();
    commentService.getComments.mockReset();
    commentService.addComment.mockReset();
    historyService.getHistory.mockReset();
    slaService.getTicketSla.mockReset();
    userService.getSupportAgents.mockReset();
    router.navigate.mockReset();
    role = 'USER';
  });

  it('loads the ticket identified by the route', async () => { await render(); expect(ticketService.getTicketById).toHaveBeenCalledWith('ticket-1'); });
  it('loads comments during direct detail-page initialization', async () => { await render(); expect(commentService.getComments).toHaveBeenCalledWith('ticket-1'); });
  it('displays the ticket number and title', async () => { const f = await render(); expect(f.nativeElement.textContent).toContain('INC-000001'); expect(f.nativeElement.textContent).toContain('API de paiement indisponible'); });
  it('displays Non assigné when no agent is assigned', async () => { expect((await render()).nativeElement.textContent).toContain('Non assigné'); });
  it('displays the assigned agent and role', async () => {
    const assigned = { ...ticket, assignedTo: { id: 'support-1', firstName: 'Nicolas', lastName: 'Support', email: 'nicolas@supportflow.local', role: 'SUPPORT_N1' as const } };
    const f = await render(of(assigned)); expect(f.nativeElement.textContent).toContain('Nicolas Support'); expect(f.nativeElement.textContent).toContain('SUPPORT_N1');
  });
  it('does not display null resolution or closing dates', async () => { const text = (await render()).nativeElement.textContent; expect(text).not.toContain('Résolu le'); expect(text).not.toContain('Fermé le'); });
  it('displays the dedicated 404 message', async () => { const f = await render(throwError(() => new HttpErrorResponse({ status: 404 }))); expect(f.nativeElement.textContent).toContain('Ticket introuvable'); });
  it('displays the dedicated 403 ticket message', async () => { const f = await render(throwError(() => new HttpErrorResponse({ status: 403 }))); expect(f.nativeElement.textContent).toContain("Vous n'avez pas accès à ce ticket"); });
  it('navigates back to the ticket list', async () => { const f = await render(); (f.nativeElement.querySelector('button.back') as HTMLButtonElement).click(); expect(router.navigate).toHaveBeenCalledWith(['/app/tickets']); });

  it('displays a public comment', async () => { const f = await render(of(ticket), of([publicComment])); expect(f.nativeElement.textContent).toContain('Information publique'); expect(f.nativeElement.textContent).toContain('Alice Martin'); });
  it('keeps the ticket and comments visible when history fails', async () => {
    const f = await render(of(ticket), of([publicComment]), throwError(() => new Error('history')));
    expect(f.nativeElement.textContent).toContain('INC-000001');
    expect(f.nativeElement.textContent).toContain('Information publique');
    expect(f.nativeElement.textContent).toContain("Impossible de charger l'historique.");
  });
  it('keeps ticket, comments and history visible when SLA fails', async () => {
    const f = await render(of(ticket), of([publicComment]), of([]), throwError(() => new HttpErrorResponse({ status: 500 })));
    expect(f.nativeElement.textContent).toContain('INC-000001');
    expect(f.nativeElement.textContent).toContain('Information publique');
    expect(f.nativeElement.textContent).toContain('Historique');
    expect(f.nativeElement.textContent).toContain('Impossible de charger les informations SLA.');
  });
  it('shows USER only the public comments returned by the backend', async () => { const f = await render(of(ticket), of([publicComment])); expect(f.nativeElement.textContent).toContain('Information publique'); expect(f.nativeElement.textContent).not.toContain('Note interne'); });
  it('displays the internal-note badge when returned by the API', async () => { const f = await render(of(ticket), of([internalComment])); expect(f.nativeElement.textContent).toContain('Note interne'); expect(f.nativeElement.textContent).toContain('Diagnostic interne'); });
  it('displays the empty-comments state', async () => { expect((await render()).nativeElement.textContent).toContain('Aucun commentaire pour le moment.'); });
  it('does not show the internal selector to USER', async () => { const f = await render(); expect(f.nativeElement.querySelector('.visibility-choice')).toBeNull(); });
  it('shows the internal selector to SUPPORT_N1', async () => { role = 'SUPPORT_N1'; const f = await render(); expect(f.nativeElement.querySelector('.visibility-choice')).not.toBeNull(); });
  it('does not show support actions to USER', async()=>{const f=await render();expect(f.nativeElement.querySelector('app-ticket-actions')).toBeNull();});
  it('refreshes ticket, SLA and history but not comments after an action',async()=>{role='SUPPORT_N1';const f=await render();const action=f.debugElement.query(e=>e.componentInstance instanceof TicketActionsComponent).componentInstance as TicketActionsComponent;action.actionSucceeded.emit({...ticket,status:'IN_PROGRESS'});expect(ticketService.getTicketById).toHaveBeenCalledTimes(2);expect(slaService.getTicketSla).toHaveBeenCalledTimes(2);expect(historyService.getHistory).toHaveBeenCalledTimes(2);expect(commentService.getComments).toHaveBeenCalledTimes(1);});
  it('keeps a CLOSED ticket fully readable for USER without actions or comment form',async()=>{const closed={...ticket,status:'CLOSED' as const,resolvedAt:'2026-08-21T18:00:00Z',closedAt:'2026-08-21T19:00:00Z'};const f=await render(of(closed),of([publicComment]));const text=f.nativeElement.textContent;expect(text).toContain('Fermé');expect(text).toContain('Résolu le');expect(text).toContain('Fermé le');expect(text).toContain(ticket.description);expect(text).toContain('Informations générales');expect(text).toContain('SLA');expect(text).toContain('Historique');expect(text).toContain('Information publique');expect(f.nativeElement.querySelector('app-ticket-actions')).toBeNull();expect(f.nativeElement.querySelector('form.comment-form')).toBeNull();expect(text).toContain('Aucun nouveau commentaire ne peut être ajouté');});
  it('refreshes related data after RESOLVED to CLOSED without reloading comments',async()=>{role='SUPPORT_N2';const resolved={...ticket,status:'RESOLVED' as const,resolvedAt:'2026-08-21T18:00:00Z'};const closed={...resolved,status:'CLOSED' as const,closedAt:'2026-08-21T19:00:00Z'};const f=await render(of(resolved),of([publicComment]));ticketService.getTicketById.mockReturnValue(of(closed));const action=f.debugElement.query(e=>e.componentInstance instanceof TicketActionsComponent).componentInstance as TicketActionsComponent;action.actionSucceeded.emit(closed);f.detectChanges();expect(f.componentInstance.ticket()?.status).toBe('CLOSED');expect(f.componentInstance.ticket()?.resolvedAt).toBe(resolved.resolvedAt);expect(f.componentInstance.ticket()?.closedAt).toBe(closed.closedAt);expect(slaService.getTicketSla).toHaveBeenCalledTimes(2);expect(historyService.getHistory).toHaveBeenCalledTimes(2);expect(commentService.getComments).toHaveBeenCalledTimes(1);});

  it('binds textarea input to the content control and updates the counter', async () => {
    const f = await render();
    const textarea = f.nativeElement.querySelector('#comment-content') as HTMLTextAreaElement;
    textarea.value = 'TEST NICOLAS PUBLIC 18H30';
    textarea.dispatchEvent(new Event('input'));
    f.detectChanges();
    expect(f.componentInstance.content.value).toBe('TEST NICOLAS PUBLIC 18H30');
    expect(f.nativeElement.querySelector('.form-meta').textContent).toContain('25 / 5000');
  });

  it('submits exactly the public text entered in the textarea', async () => {
    role = 'SUPPORT_N1'; commentService.addComment.mockReturnValue(new Subject<CommentResponse>()); const f = await render();
    const textarea = f.nativeElement.querySelector('#comment-content') as HTMLTextAreaElement;
    textarea.value = 'TEST NICOLAS PUBLIC 18H30'; textarea.dispatchEvent(new Event('input')); f.detectChanges();
    (f.nativeElement.querySelector('button.publish') as HTMLButtonElement).click();
    expect(commentService.addComment).toHaveBeenCalledWith('ticket-1', { content: 'TEST NICOLAS PUBLIC 18H30', internal: false });
  });

  it('submits exactly the internal text entered in the textarea', async () => {
    role = 'SUPPORT_N1'; commentService.addComment.mockReturnValue(new Subject<CommentResponse>()); const f = await render();
    const radios = f.nativeElement.querySelectorAll('.visibility-choice input') as NodeListOf<HTMLInputElement>;
    radios[1].click();
    const textarea = f.nativeElement.querySelector('#comment-content') as HTMLTextAreaElement;
    textarea.value = 'TEST NICOLAS INTERNE 18H31'; textarea.dispatchEvent(new Event('input')); f.detectChanges();
    (f.nativeElement.querySelector('button.publish') as HTMLButtonElement).click();
    expect(commentService.addComment).toHaveBeenCalledWith('ticket-1', { content: 'TEST NICOLAS INTERNE 18H31', internal: true });
  });

  it('always submits internal=false for USER', async () => {
    commentService.addComment.mockReturnValue(of(publicComment)); const f = await render();
    f.componentInstance.internal.set(true); f.componentInstance.content.setValue(' Message public '); f.componentInstance.publish();
    expect(commentService.addComment).toHaveBeenCalledWith('ticket-1', { content: 'Message public', internal: false });
  });
  it('allows support to submit an internal note', async () => {
    role = 'SUPPORT_N1'; commentService.addComment.mockReturnValue(of(internalComment)); const f = await render();
    f.componentInstance.setInternal(true); f.componentInstance.content.setValue('Diagnostic interne'); f.componentInstance.publish();
    expect(commentService.addComment).toHaveBeenCalledWith('ticket-1', { content: 'Diagnostic interne', internal: true });
  });
  it('prevents publishing empty content', async () => { const f = await render(); f.componentInstance.content.setValue('   '); f.componentInstance.publish(); expect(commentService.addComment).not.toHaveBeenCalled(); });
  it('rejects content longer than 5000 characters', async () => { const f = await render(); f.componentInstance.content.setValue('x'.repeat(5001)); expect(f.componentInstance.content.invalid).toBe(true); f.componentInstance.publish(); expect(commentService.addComment).not.toHaveBeenCalled(); });
  it('adds the returned comment immediately and clears the form', async () => {
    commentService.addComment.mockReturnValue(of(publicComment)); commentService.getComments.mockReturnValueOnce(of([])).mockReturnValueOnce(of([publicComment])); const f = await render(); f.componentInstance.content.setValue('Information publique'); f.componentInstance.publish(); f.detectChanges();
    expect(f.componentInstance.comments()).toContain(publicComment); expect(f.componentInstance.content.value).toBe(''); expect(f.nativeElement.textContent).toContain('Information publique');
    expect(commentService.getComments).toHaveBeenCalledTimes(2);
  });
  it('prevents a double submission while publishing', async () => {
    const pending = new Subject<CommentResponse>(); commentService.addComment.mockReturnValue(pending); const f = await render(); f.componentInstance.content.setValue('Message');
    f.componentInstance.publish(); f.componentInstance.publish(); expect(commentService.addComment).toHaveBeenCalledTimes(1); pending.next(publicComment); pending.complete();
  });
  it('does not reset the form before the HTTP request succeeds', async () => {
    const pending = new Subject<CommentResponse>(); commentService.addComment.mockReturnValue(pending); const f = await render();
    f.componentInstance.content.setValue('Message conservé'); f.componentInstance.publish();
    expect(f.componentInstance.content.value).toBe('Message conservé');
    expect(f.componentInstance.publishing()).toBe(true);
  });
  it('keeps the entered content when publication fails', async () => {
    commentService.addComment.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 422 }))); const f = await render();
    f.componentInstance.content.setValue('Message à corriger'); f.componentInstance.publish();
    expect(f.componentInstance.content.value).toBe('Message à corriger');
  });
  it('displays the publication message for 403', async () => {
    commentService.addComment.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 403 }))); const f = await render(); f.componentInstance.content.setValue('Message'); f.componentInstance.publish(); f.detectChanges();
    expect(f.nativeElement.textContent).toContain("Vous n'êtes pas autorisé à effectuer cette action.");
  });
  it('displays the publication message for 422', async () => {
    commentService.addComment.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 422 }))); const f = await render(); f.componentInstance.content.setValue('Message'); f.componentInstance.publish(); f.detectChanges();
    expect(f.nativeElement.textContent).toContain('Ce commentaire ne peut pas être ajouté.');
  });
});
