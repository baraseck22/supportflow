package com.baraseck.supportflow.service;

import com.baraseck.supportflow.dto.AddCommentRequest;
import com.baraseck.supportflow.dto.CommentResponse;

import java.util.List;
import java.util.UUID;

public interface CommentService {
    CommentResponse addComment(UUID ticketId, AddCommentRequest request);
    List<CommentResponse> getComments(UUID ticketId);
}
