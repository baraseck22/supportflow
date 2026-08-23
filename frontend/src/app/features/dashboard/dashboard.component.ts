import { DatePipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { DashboardSummary, DashboardTicket } from '../../core/models/dashboard.models';
import { TicketPriority, TicketStatus } from '../../core/models/ticket.models';
import { DashboardService } from '../../core/services/dashboard.service';

@Component({selector:'app-dashboard',imports:[DatePipe],templateUrl:'./dashboard.component.html',styleUrl:'./dashboard.component.scss'})
export class DashboardComponent implements OnInit {
  private readonly service=inject(DashboardService);private readonly router=inject(Router);
  readonly summary=signal<DashboardSummary|null>(null);readonly loading=signal(true);readonly error=signal(false);
  ngOnInit():void{this.load();}
  load():void{this.loading.set(true);this.error.set(false);this.service.getSummary().subscribe({next:value=>{this.summary.set(value);this.loading.set(false);},error:()=>{this.error.set(true);this.loading.set(false);}});}
  filter(queryParams:Record<string,string|boolean>):void{void this.router.navigate(['/app/tickets'],{queryParams});}
  open(ticket:DashboardTicket):void{void this.router.navigate(['/app/tickets',ticket.id]);}
  statusLabel(status:TicketStatus):string{return({NEW:'Nouveau',IN_PROGRESS:'En cours',WAITING:'En attente',ESCALATED:'Escaladé',RESOLVED:'Résolu',CLOSED:'Fermé'}as const)[status];}
  priorityLabel(priority:TicketPriority):string{return({LOW:'Faible',MEDIUM:'Moyenne',HIGH:'Haute',CRITICAL:'Critique'}as const)[priority];}
  noData(value:DashboardSummary):boolean{return value.totalOpen===0&&value.recentTickets.length===0;}
}
