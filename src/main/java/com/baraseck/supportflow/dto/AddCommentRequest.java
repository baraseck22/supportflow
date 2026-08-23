package com.baraseck.supportflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddCommentRequest(@NotBlank @Size(max = 5000) String content, boolean internal) {
    public AddCommentRequest(java.util.UUID ignored, String content, boolean internal) { this(content, internal); }
}
