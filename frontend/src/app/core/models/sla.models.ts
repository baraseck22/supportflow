export type SlaStatus = 'ON_TIME' | 'AT_RISK' | 'BREACHED' | 'COMPLETED';

export interface SlaSummary {
  responseDueAt: string;
  resolutionDueAt: string;
  firstResponseAt: string | null;
  resolvedAt: string | null;
  responseStatus: SlaStatus;
  resolutionStatus: SlaStatus;
  responseRemainingSeconds: number | null;
  resolutionRemainingSeconds: number | null;
}
