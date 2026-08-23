package com.baraseck.supportflow.dto;

import com.baraseck.supportflow.entity.TicketStatus;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ChangeTicketStatusRequest(@NotNull TicketStatus status) {
    public ChangeTicketStatusRequest(TicketStatus status, UUID ignored) { this(status); }
}
