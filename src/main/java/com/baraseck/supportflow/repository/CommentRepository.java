package com.baraseck.supportflow.repository;

import com.baraseck.supportflow.entity.Comment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, UUID> {
    @EntityGraph(attributePaths = "author")
    List<Comment> findByTicketIdOrderByCreatedAtAscIdAsc(UUID ticketId);
}
