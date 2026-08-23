package com.baraseck.supportflow.dto;

import com.baraseck.supportflow.entity.TicketPriority;
import com.baraseck.supportflow.entity.TicketStatus;

import java.time.Instant;
import java.util.UUID;

public record TicketResponse(
        UUID id,
        String ticketNumber,
        String title,
        String description,
        TicketStatus status,
        TicketPriority priority,
        CategorySummary category,
        UserSummary createdBy,
        UserSummary assignedTo,
        Instant createdAt,
        Instant updatedAt,
        Instant resolvedAt,
        Instant closedAt,
        Instant firstResponseAt,
        Instant responseDueAt,
        Instant resolutionDueAt) {
}
