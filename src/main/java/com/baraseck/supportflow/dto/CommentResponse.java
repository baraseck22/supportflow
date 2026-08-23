package com.baraseck.supportflow.dto;

import java.time.Instant;
import java.util.UUID;

public record CommentResponse(
        UUID id, String content, boolean internal, UserSummary author,
        Instant createdAt, Instant updatedAt) {
}
