package com.baraseck.supportflow.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssignTicketRequest(@NotNull UUID assignedToUserId) {
    public AssignTicketRequest(UUID assignedToUserId, UUID ignored) { this(assignedToUserId); }
}
