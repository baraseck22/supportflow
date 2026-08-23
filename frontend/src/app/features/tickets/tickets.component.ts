import { DatePipe } from '@angular/common';
import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, ParamMap, Router } from '@angular/router';
import { debounceTime, distinctUntilChanged, merge } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AuthService } from '../../core/auth/auth.service';
import { AuthenticatedUser } from '../../core/models/auth.models';
import { Ticket, TicketPriority, TicketQuery, TicketStatus } from '../../core/models/ticket.models';
import { TicketService } from '../../core/services/ticket.service';
import { UserService } from '../../core/services/user.service';

@Component({selector:'app-tickets',imports:[DatePipe,ReactiveFormsModule],templateUrl:'./tickets.component.html',styleUrl:'./tickets.component.scss'})
export class TicketsComponent implements OnInit {
  private readonly service=inject(TicketService);private readonly users=inject(UserService);private readonly auth=inject(AuthService);private readonly router=inject(Router);private readonly route=inject(ActivatedRoute);private readonly destroyRef=inject(DestroyRef);
  readonly tickets=signal<Ticket[]>([]);readonly agents=signal<AuthenticatedUser[]>([]);readonly loading=signal(true);readonly agentsError=signal(false);readonly error=signal('');readonly page=signal(0);readonly totalPages=signal(0);readonly totalElements=signal(0);
  readonly currentUser=this.auth.getCurrentUser();readonly isSupport=this.currentUser?.role!=='USER';readonly title=this.isSupport?'Tickets':'Mes tickets';
  readonly filters=new FormGroup({search:new FormControl('',{nonNullable:true}),status:new FormControl('',{nonNullable:true}),priority:new FormControl('',{nonNullable:true}),assignedTo:new FormControl('',{nonNullable:true}),size:new FormControl(20,{nonNullable:true}),sort:new FormControl('createdAt,desc',{nonNullable:true})});

  ngOnInit():void{
    this.filters.controls.search.valueChanges.pipe(debounceTime(350),distinctUntilChanged(),takeUntilDestroyed(this.destroyRef)).subscribe(()=>this.filtersChanged());
    merge(this.filters.controls.status.valueChanges,this.filters.controls.priority.valueChanges,this.filters.controls.assignedTo.valueChanges,this.filters.controls.size.valueChanges,this.filters.controls.sort.valueChanges).pipe(takeUntilDestroyed(this.destroyRef)).subscribe(()=>this.filtersChanged());
    this.route.queryParamMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(params=>this.restoreAndLoad(params));
    if(this.isSupport)this.loadAgents();
  }
  private restoreAndLoad(params:ParamMap):void{
    const size=[10,20,50].includes(Number(params.get('size')))?Number(params.get('size')):20;
    const sort=['createdAt,desc','createdAt,asc','priority,desc','status,asc'].includes(params.get('sort')??'')?params.get('sort')!:'createdAt,desc';
    this.filters.patchValue({search:params.get('search')??'',status:params.get('status')??'',priority:params.get('priority')??'',assignedTo:this.isSupport?(params.get('unassigned')==='true'?'UNASSIGNED':params.get('assignedTo')??''):'',size,sort},{emitEvent:false});
    this.page.set(Math.max(0,Number(params.get('page'))||0));this.loadTickets();
  }
  private filtersChanged():void{void this.router.navigate([],{relativeTo:this.route,queryParams:this.queryParams(0)});}
  private queryParams(page:number):Record<string,string|number|null>{const value=this.filters.getRawValue();return{search:value.search.trim()||null,status:value.status||null,priority:value.priority||null,assignedTo:this.isSupport&&value.assignedTo&&value.assignedTo!=='UNASSIGNED'?value.assignedTo:null,unassigned:this.isSupport&&value.assignedTo==='UNASSIGNED'?'true':null,page,size:value.size,sort:value.sort};}
  private query():TicketQuery{const value=this.filters.getRawValue();return{page:this.page(),size:value.size,sort:value.sort,search:value.search||undefined,status:(value.status||undefined)as TicketStatus|undefined,priority:(value.priority||undefined)as TicketPriority|undefined,assignedTo:this.isSupport&&value.assignedTo&&value.assignedTo!=='UNASSIGNED'?value.assignedTo:undefined,unassigned:this.isSupport&&value.assignedTo==='UNASSIGNED'};}
  loadTickets():void{this.loading.set(true);this.error.set('');this.service.getTickets(this.query()).subscribe({next:response=>{this.tickets.set(response.content);this.page.set(response.number);this.totalPages.set(response.totalPages);this.totalElements.set(response.totalElements);this.loading.set(false);},error:()=>{this.error.set('Impossible de charger les tickets. Réessayez dans quelques instants.');this.loading.set(false);}});}
  private loadAgents():void{this.agentsError.set(false);this.users.getSupportAgents().subscribe({next:value=>this.agents.set(value),error:()=>this.agentsError.set(true)});}
  resetFilters():void{this.filters.setValue({search:'',status:'',priority:'',assignedTo:'',size:20,sort:'createdAt,desc'},{emitEvent:false});void this.router.navigate([],{relativeTo:this.route,queryParams:{page:0,size:20,sort:'createdAt,desc'}});}
  changePage(page:number):void{if(page>=0&&page<this.totalPages()&&page!==this.page())void this.router.navigate([],{relativeTo:this.route,queryParams:this.queryParams(page)});}
  hasActiveFilters():boolean{const v=this.filters.getRawValue();return!!(v.search.trim()||v.status||v.priority||(this.isSupport&&v.assignedTo));}
  newTicket():void{void this.router.navigate(['/app/tickets/new']);}openTicket(id:string):void{void this.router.navigate(['/app/tickets',id]);}
  statusLabel(status:TicketStatus):string{return({NEW:'Nouveau',IN_PROGRESS:'En cours',WAITING:'En attente',ESCALATED:'Escaladé',RESOLVED:'Résolu',CLOSED:'Fermé'}as const)[status];}
  priorityLabel(priority:TicketPriority):string{return({LOW:'Faible',MEDIUM:'Moyenne',HIGH:'Haute',CRITICAL:'Critique'}as const)[priority];}
}
