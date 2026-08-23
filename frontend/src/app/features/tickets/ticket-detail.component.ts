import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, OnInit, signal, ViewChild } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { CommentResponse } from '../../core/models/comment.models';
import { Ticket } from '../../core/models/ticket.models';
import { CommentService } from '../../core/services/comment.service';
import { TicketService } from '../../core/services/ticket.service';
import { TicketHistoryComponent } from './ticket-history/ticket-history.component';
import { TicketSlaComponent } from './ticket-sla/ticket-sla.component';
import { TicketActionsComponent } from './ticket-actions/ticket-actions.component';

@Component({
  selector: 'app-ticket-detail',
  imports: [DatePipe, ReactiveFormsModule, TicketHistoryComponent, TicketSlaComponent, TicketActionsComponent],
  templateUrl: './ticket-detail.component.html',
  styleUrl: './ticket-detail.component.scss'
})
export class TicketDetailComponent implements OnInit {
  @ViewChild(TicketSlaComponent) private slaComponent?: TicketSlaComponent;
  @ViewChild(TicketHistoryComponent) private historyComponent?: TicketHistoryComponent;
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly ticketService = inject(TicketService);
  private readonly commentService = inject(CommentService);
  private readonly authService = inject(AuthService);
  private ticketId = '';

  readonly ticket = signal<Ticket | null>(null);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly comments = signal<CommentResponse[]>([]);
  readonly commentsLoading = signal(false);
  readonly commentsError = signal('');
  readonly publishing = signal(false);
  readonly publishError = signal('');
  readonly internal = signal(false);
  readonly commentForm = new FormGroup({
    content: new FormControl('', {
      nonNullable: true,
      validators: [
        Validators.required,
        Validators.maxLength(5000),
        control => control.value.trim() ? null : { blank: true }
      ]
    })
  });
  readonly content = this.commentForm.controls.content;

  readonly canCreateInternalNote = ['SUPPORT_N1', 'SUPPORT_N2', 'ADMIN']
    .includes(this.authService.getCurrentUser()?.role ?? '');
  readonly showSupportActions = ['SUPPORT_N1', 'SUPPORT_N2', 'ADMIN']
    .includes(this.authService.getCurrentUser()?.role ?? '');

  ngOnInit(): void {
    this.ticketId = this.route.snapshot.paramMap.get('id') ?? '';
    if (!this.ticketId) {
      this.error.set('Ticket introuvable');
      this.loading.set(false);
      return;
    }
    this.ticketService.getTicketById(this.ticketId).subscribe({
      next: ticket => {
        this.ticket.set(ticket);
        this.loading.set(false);
        this.loadComments();
      },
      error: (error: HttpErrorResponse) => {
        this.error.set(error.status === 404 ? 'Ticket introuvable' : error.status === 403
          ? "Vous n'avez pas accès à ce ticket"
          : 'Impossible de charger ce ticket. Réessayez dans quelques instants.');
        this.loading.set(false);
      }
    });
  }

  loadComments(): void {
    this.commentsLoading.set(true);
    this.commentsError.set('');
    this.commentService.getComments(this.ticketId).subscribe({
      next: comments => {
        this.comments.set(this.sortComments(comments));
        this.commentsLoading.set(false);
      },
      error: () => {
        this.commentsError.set('Impossible de charger les commentaires.');
        this.commentsLoading.set(false);
      }
    });
  }

  publish(): void {
    this.content.markAsTouched();
    if (this.content.invalid || this.publishing()) return;
    this.publishing.set(true);
    this.publishError.set('');
    this.commentService.addComment(this.ticketId, {
      content: this.content.value.trim(),
      internal: this.canCreateInternalNote ? this.internal() : false
    }).subscribe({
      next: comment => {
        // The GET response is the authoritative, role-filtered view. Refreshing it
        // also prevents a POST response from being duplicated in the displayed list.
        this.comments.update(comments => this.mergeComment(comments, comment));
        this.commentForm.reset({ content: '' });
        this.internal.set(false);
        this.publishing.set(false);
        this.loadComments();
      },
      error: (error: HttpErrorResponse) => {
        this.publishError.set(error.status === 403
          ? "Vous n'êtes pas autorisé à effectuer cette action."
          : error.status === 422 ? 'Ce commentaire ne peut pas être ajouté.'
          : "Une erreur est survenue lors de l'ajout du commentaire.");
        this.publishing.set(false);
      }
    });
  }

  setInternal(value: boolean): void { this.internal.set(value); }
  handleActionSuccess(updated: Ticket): void {
    this.ticket.set(updated);
    this.slaComponent?.load();
    this.historyComponent?.load();
    this.ticketService.getTicketById(updated.id).subscribe({ next: ticket => this.ticket.set(ticket) });
  }
  back(): void { void this.router.navigate(['/app/tickets']); }
  statusLabel(status: Ticket['status']): string { return ({ NEW: 'Nouveau', IN_PROGRESS: 'En cours', WAITING: 'En attente', ESCALATED: 'Escaladé', RESOLVED: 'Résolu', CLOSED: 'Fermé' } as const)[status]; }
  priorityLabel(priority: Ticket['priority']): string { return ({ LOW: 'Basse', MEDIUM: 'Moyenne', HIGH: 'Haute', CRITICAL: 'Critique' } as const)[priority]; }

  private sortComments(comments: CommentResponse[]): CommentResponse[] {
    return [...comments].sort((a, b) => a.createdAt.localeCompare(b.createdAt) || a.id.localeCompare(b.id));
  }

  private mergeComment(comments: CommentResponse[], comment: CommentResponse): CommentResponse[] {
    return this.sortComments([...comments.filter(item => item.id !== comment.id), comment]);
  }
}
