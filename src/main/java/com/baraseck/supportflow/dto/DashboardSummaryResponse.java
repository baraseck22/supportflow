package com.baraseck.supportflow.dto;

import java.util.List;

public record DashboardSummaryResponse(
        long totalOpen,
        long newTickets,
        long inProgress,
        long pending,
        long escalated,
        long criticalOpen,
        long unassigned,
        long responseSlaAtRisk,
        long responseSlaBreached,
        long resolutionSlaAtRisk,
        long resolutionSlaBreached,
        List<DashboardTicketSummary> priorityTickets,
        List<DashboardTicketSummary> recentTickets
) {}
