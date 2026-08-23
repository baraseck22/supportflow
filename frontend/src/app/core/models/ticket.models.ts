import { AuthenticatedUser } from './auth.models';
export type TicketStatus='NEW'|'IN_PROGRESS'|'WAITING'|'ESCALATED'|'RESOLVED'|'CLOSED';
export type TicketPriority='LOW'|'MEDIUM'|'HIGH'|'CRITICAL';
export interface CategorySummary{id:string;name:string;description:string|null}
export interface Ticket{id:string;ticketNumber:string;title:string;description:string;status:TicketStatus;priority:TicketPriority;category:CategorySummary;createdBy:AuthenticatedUser;assignedTo:AuthenticatedUser|null;createdAt:string;updatedAt:string;resolvedAt:string|null;closedAt:string|null;firstResponseAt:string|null;responseDueAt:string;resolutionDueAt:string}
export interface PageResponse<T>{content:T[];totalElements:number;totalPages:number;size:number;number:number;first:boolean;last:boolean}
export interface CreateTicketRequest{title:string;description:string;priority:TicketPriority;categoryId:string}
export interface TicketQuery{page:number;size:number;sort:string;search?:string;status?:TicketStatus;priority?:TicketPriority;assignedTo?:string;unassigned?:boolean}
