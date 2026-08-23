package com.baraseck.supportflow.mapper;

import com.baraseck.supportflow.dto.CommentResponse;
import com.baraseck.supportflow.entity.Comment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommentMapper {
    private final UserSummaryMapper userSummaryMapper;

    public CommentResponse toResponse(Comment comment) {
        return new CommentResponse(comment.getId(), comment.getContent(), comment.isInternal(),
                userSummaryMapper.toSummary(comment.getAuthor()),
                comment.getCreatedAt(), comment.getUpdatedAt());
    }
}
