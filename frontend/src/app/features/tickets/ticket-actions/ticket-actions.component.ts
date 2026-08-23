import { HttpErrorResponse } from '@angular/common/http';
import { Component, EventEmitter, inject, Input, OnChanges, OnInit, Output, signal, SimpleChanges } from '@angular/core';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../../core/auth/auth.service';
import { AuthenticatedUser, Role } from '../../../core/models/auth.models';
import { Ticket, TicketStatus } from '../../../core/models/ticket.models';
import { TicketService } from '../../../core/services/ticket.service';
import { UserService } from '../../../core/services/user.service';

@Component({selector:'app-ticket-actions',imports:[ReactiveFormsModule],templateUrl:'./ticket-actions.component.html',styleUrl:'./ticket-actions.component.scss'})
export class TicketActionsComponent implements OnInit,OnChanges{
  @Input({required:true})ticket!:Ticket;
  @Output()actionSucceeded=new EventEmitter<Ticket>();
  private readonly ticketService=inject(TicketService);private readonly userService=inject(UserService);private readonly auth=inject(AuthService);
  readonly role:Role=this.auth.getCurrentUser()?.role??'USER';
  readonly agents=signal<AuthenticatedUser[]>([]);readonly agentsLoading=signal(false);readonly agentsError=signal('');
  readonly busy=signal<'status'|'assign'|'escalate'|null>(null);readonly actionError=signal('');
  readonly statusControl=new FormControl<TicketStatus|null>(null,Validators.required);
  readonly assigneeControl=new FormControl('',Validators.required);
  readonly targetControl=new FormControl<string|null>(null);
  readonly reasonControl=new FormControl('',{nonNullable:true,validators:[Validators.required,Validators.maxLength(1000),c=>c.value.trim()?null:{blank:true}]});
  readonly statusLabels:Record<TicketStatus,string>={NEW:'Nouveau',IN_PROGRESS:'En cours',WAITING:'En attente',ESCALATED:'Escaladé',RESOLVED:'Résolu',CLOSED:'Fermé'};
  get canAssign(){return this.role==='SUPPORT_N1'||this.role==='ADMIN'}get canEscalate(){return this.role==='SUPPORT_N1'||this.role==='ADMIN'}get closed(){return this.ticket.status==='CLOSED'}
  get transitions():TicketStatus[]{return({NEW:['IN_PROGRESS','CLOSED'],IN_PROGRESS:['WAITING','RESOLVED','CLOSED'],WAITING:['IN_PROGRESS','CLOSED'],ESCALATED:['IN_PROGRESS','RESOLVED','CLOSED'],RESOLVED:['IN_PROGRESS','CLOSED'],CLOSED:[]}as Record<TicketStatus,TicketStatus[]>)[this.ticket.status]}
  get escalationTargets(){return this.agents().filter(a=>a.role==='SUPPORT_N2'||a.role==='ADMIN')}
  ngOnInit(){if(this.canAssign||this.canEscalate)this.loadAgents();this.syncControls()}
  ngOnChanges(changes:SimpleChanges){if(changes['ticket'])this.syncControls()}
  loadAgents(){this.agentsLoading.set(true);this.userService.getSupportAgents().subscribe({next:a=>{this.agents.set(a);this.agentsLoading.set(false)},error:()=>{this.agentsError.set('Impossible de charger les agents support.');this.agentsLoading.set(false)}})}
  changeStatus(){const status=this.statusControl.value;if(!status||this.busy())return;this.run('status',this.ticketService.changeStatus(this.ticket.id,status))}
  assign(){const id=this.assigneeControl.value;if(!id||this.busy())return;this.run('assign',this.ticketService.assignTicket(this.ticket.id,id))}
  escalate(){this.reasonControl.markAsTouched();if(this.reasonControl.invalid||this.busy())return;this.run('escalate',this.ticketService.escalateTicket(this.ticket.id,this.targetControl.value||null,this.reasonControl.value.trim()),()=>{this.reasonControl.reset('');this.targetControl.reset(null)})}
  private run(kind:'status'|'assign'|'escalate',request:ReturnType<TicketService['changeStatus']>,after?:()=>void){this.busy.set(kind);this.actionError.set('');request.subscribe({next:ticket=>{this.busy.set(null);after?.();this.actionSucceeded.emit(ticket)},error:(e:HttpErrorResponse)=>{this.busy.set(null);this.actionError.set(this.message(e.status,kind))}})}
  private message(status:number,kind:string){if(status===403)return"Vous n'êtes pas autorisé à effectuer cette action.";if(status===409)return kind==='status'?"Cette transition de statut n'est pas autorisée.":"Cette action n'est pas autorisée dans l'état actuel du ticket.";if(status===422)return"Cette action ne peut pas être effectuée.";return"Une erreur est survenue pendant l'action."}
  private syncControls(){if(!this.ticket)return;this.assigneeControl.setValue(this.ticket.assignedTo?.id??'');this.statusControl.setValue(null)}
}
