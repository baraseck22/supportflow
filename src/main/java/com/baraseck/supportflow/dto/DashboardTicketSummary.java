package com.baraseck.supportflow.dto;

import com.baraseck.supportflow.entity.TicketPriority;
import com.baraseck.supportflow.entity.TicketStatus;

import java.time.Instant;
import java.util.UUID;

public record DashboardTicketSummary(
        UUID id,
        String ticketNumber,
        String title,
        TicketStatus status,
        TicketPriority priority,
        UserSummary assignedTo,
        Instant createdAt
) {}
