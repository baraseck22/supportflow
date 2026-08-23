import { AuthenticatedUser } from './auth.models';
import { TicketPriority, TicketStatus } from './ticket.models';
export interface DashboardTicket{id:string;ticketNumber:string;title:string;status:TicketStatus;priority:TicketPriority;assignedTo:AuthenticatedUser|null;createdAt:string}
export interface DashboardSummary{totalOpen:number;newTickets:number;inProgress:number;pending:number;escalated:number;criticalOpen:number;unassigned:number;responseSlaAtRisk:number;responseSlaBreached:number;resolutionSlaAtRisk:number;resolutionSlaBreached:number;priorityTickets:DashboardTicket[];recentTickets:DashboardTicket[]}
