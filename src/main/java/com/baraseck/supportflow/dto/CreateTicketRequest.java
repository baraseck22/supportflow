package com.baraseck.supportflow.dto;

import com.baraseck.supportflow.entity.TicketPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateTicketRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank String description,
        @NotNull TicketPriority priority,
        @NotNull UUID categoryId) {
    public CreateTicketRequest(String title, String description, TicketPriority priority, UUID categoryId, UUID ignored) {
        this(title, description, priority, categoryId);
    }
}
