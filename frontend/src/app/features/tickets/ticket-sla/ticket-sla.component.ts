import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, Input, OnInit, signal } from '@angular/core';
import { SlaStatus, SlaSummary } from '../../../core/models/sla.models';
import { SlaService } from '../../../core/services/sla.service';

export function formatRemainingSeconds(seconds: number | null): string {
  if (seconds === null) return 'Non disponible';
  const value = Math.max(0, Math.floor(seconds));
  const days = Math.floor(value / 86400);
  const hours = Math.floor(value % 86400 / 3600);
  const minutes = Math.floor(value % 3600 / 60);
  const remainingSeconds = value % 60;
  if (days) return `${days} j${hours ? ` ${hours} h` : ''}`;
  if (hours) return `${hours} h${minutes ? ` ${minutes} min` : ''}`;
  if (minutes) return `${minutes} min${remainingSeconds ? ` ${remainingSeconds} s` : ''}`;
  return `${remainingSeconds} s`;
}

@Component({
  selector: 'app-ticket-sla',
  imports: [DatePipe],
  templateUrl: './ticket-sla.component.html',
  styleUrl: './ticket-sla.component.scss'
})
export class TicketSlaComponent implements OnInit {
  @Input({ required: true }) ticketId = '';
  private readonly slaService = inject(SlaService);
  readonly sla = signal<SlaSummary | null>(null);
  readonly loading = signal(true);
  readonly error = signal('');

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading.set(true);
    this.error.set('');
    this.slaService.getTicketSla(this.ticketId).subscribe({
      next: sla => { this.sla.set(sla); this.loading.set(false); },
      error: (error: HttpErrorResponse) => {
        this.error.set(error.status === 403 ? "Vous n'avez pas accès aux informations SLA."
          : error.status === 404 ? 'Informations SLA introuvables.'
          : 'Impossible de charger les informations SLA.');
        this.loading.set(false);
      }
    });
  }

  statusLabel(status: SlaStatus): string {
    return ({ ON_TIME: 'Dans les délais', AT_RISK: 'À risque', BREACHED: 'SLA dépassé', COMPLETED: 'Terminé' })[status];
  }

  statusSymbol(status: SlaStatus): string { return status === 'AT_RISK' ? '!' : status === 'BREACHED' ? '×' : '✓'; }

  remaining(status: SlaStatus, seconds: number | null): string {
    if (status === 'COMPLETED') return 'Terminé';
    if (status === 'BREACHED' && seconds === 0) return 'Délai dépassé';
    return formatRemainingSeconds(seconds);
  }
}
