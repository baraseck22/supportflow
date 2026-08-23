package com.baraseck.supportflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record EscalateTicketRequest(
        UUID targetUserId,
        @NotBlank String reason) {
    public EscalateTicketRequest(UUID targetUserId, UUID ignored, String reason) { this(targetUserId, reason); }
}
