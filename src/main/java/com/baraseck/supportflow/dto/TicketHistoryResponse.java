package com.baraseck.supportflow.dto;

import java.time.Instant;
import java.util.UUID;

public record TicketHistoryResponse(
        UUID id,
        String fieldName,
        String oldValue,
        String newValue,
        UserSummary changedBy,
        Instant createdAt) {
}
