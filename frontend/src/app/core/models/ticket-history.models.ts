import { AuthenticatedUser } from './auth.models';

export interface TicketHistoryEntry {
  id: string;
  fieldName: string;
  oldValue: string | null;
  newValue: string | null;
  changedBy: AuthenticatedUser;
  createdAt: string;
}
