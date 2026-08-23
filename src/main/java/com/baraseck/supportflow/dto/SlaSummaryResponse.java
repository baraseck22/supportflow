package com.baraseck.supportflow.dto;

import com.baraseck.supportflow.entity.SlaStatus;

import java.time.Instant;

public record SlaSummaryResponse(
        Instant responseDueAt,
        Instant resolutionDueAt,
        Instant firstResponseAt,
        Instant resolvedAt,
        SlaStatus responseStatus,
        SlaStatus resolutionStatus,
        Long responseRemainingSeconds,
        Long resolutionRemainingSeconds) {
}
