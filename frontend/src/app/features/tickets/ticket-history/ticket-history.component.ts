import { DatePipe } from '@angular/common';
import { Component, inject, Input, OnInit, signal } from '@angular/core';
import { TicketHistoryEntry } from '../../../core/models/ticket-history.models';
import { TicketHistoryService } from '../../../core/services/ticket-history.service';

@Component({
  selector: 'app-ticket-history',
  imports: [DatePipe],
  templateUrl: './ticket-history.component.html',
  styleUrl: './ticket-history.component.scss'
})
export class TicketHistoryComponent implements OnInit {
  @Input({ required: true }) ticketId = '';
  private readonly historyService = inject(TicketHistoryService);

  readonly entries = signal<TicketHistoryEntry[]>([]);
  readonly loading = signal(true);
  readonly error = signal(false);

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading.set(true);
    this.error.set(false);
    this.historyService.getHistory(this.ticketId).subscribe({
      next: entries => {
        this.entries.set(entries);
        this.loading.set(false);
      },
      error: () => {
        this.error.set(true);
        this.loading.set(false);
      }
    });
  }

  describe(entry: TicketHistoryEntry): string {
    if (entry.fieldName === 'ticket') return `Ticket ${entry.newValue ?? ''} créé`.replace('  ', ' ').trim();
    if (entry.fieldName === 'status') return `Statut modifié : ${this.statusLabel(entry.oldValue)} → ${this.statusLabel(entry.newValue)}`;
    if (entry.fieldName === 'assignedTo') return entry.newValue ? 'Ticket affecté' : 'Affectation retirée';
    if (entry.fieldName === 'sla' && entry.newValue === 'FIRST_RESPONSE_RECORDED') return 'Première réponse SLA enregistrée';
    if (entry.fieldName === 'comment' && entry.newValue === 'PUBLIC_COMMENT_ADDED') return 'Commentaire public ajouté';
    if (entry.fieldName === 'comment' && entry.newValue === 'INTERNAL_NOTE_ADDED') return 'Note interne ajoutée';
    if (entry.fieldName === 'escalationReason') return `Motif d'escalade : ${entry.newValue ?? 'Non renseigné'}`;
    return this.fallbackDescription(entry);
  }

  private statusLabel(value: string | null): string {
    if (!value) return 'Non renseigné';
    return ({ NEW: 'Nouveau', IN_PROGRESS: 'En cours', WAITING: 'En attente', ESCALATED: 'Escaladé', RESOLVED: 'Résolu', CLOSED: 'Fermé' } as Record<string, string>)[value] ?? value;
  }

  private fallbackDescription(entry: TicketHistoryEntry): string {
    const label = entry.fieldName || 'Événement';
    if (entry.oldValue !== null && entry.newValue !== null) return `${label} modifié : ${entry.oldValue} → ${entry.newValue}`;
    if (entry.newValue !== null) return `${label} : ${entry.newValue}`;
    return `${label} modifié`;
  }
}
