import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { environment } from '../../../environments/environment';
import { CreateTicketRequest, PageResponse, Ticket, TicketQuery, TicketStatus } from '../models/ticket.models';

@Injectable({providedIn:'root'})
export class TicketService {
  private readonly http=inject(HttpClient);
  getTickets(query:Partial<TicketQuery>={}) {
    let params=new HttpParams().set('page',query.page??0).set('size',query.size??20).set('sort',query.sort??'createdAt,desc');
    if(query.search?.trim())params=params.set('search',query.search.trim());
    if(query.status)params=params.set('status',query.status);
    if(query.priority)params=params.set('priority',query.priority);
    if(query.assignedTo)params=params.set('assignedTo',query.assignedTo);
    if(query.unassigned)params=params.set('unassigned','true');
    return this.http.get<PageResponse<Ticket>>(`${environment.apiBaseUrl}/api/tickets`,{params});
  }
  getTicketById(id:string){return this.http.get<Ticket>(`${environment.apiBaseUrl}/api/tickets/${id}`);}
  createTicket(request:CreateTicketRequest){return this.http.post<Ticket>(`${environment.apiBaseUrl}/api/tickets`,request);}
  assignTicket(id:string,assignedToUserId:string){return this.http.put<Ticket>(`${environment.apiBaseUrl}/api/tickets/${id}/assign`,{assignedToUserId});}
  changeStatus(id:string,status:TicketStatus){return this.http.put<Ticket>(`${environment.apiBaseUrl}/api/tickets/${id}/status`,{status});}
  escalateTicket(id:string,targetUserId:string|null,reason:string){return this.http.put<Ticket>(`${environment.apiBaseUrl}/api/tickets/${id}/escalate`,{targetUserId,reason});}
}
