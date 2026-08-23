package com.baraseck.supportflow.repository;

import com.baraseck.supportflow.entity.TicketHistory;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TicketHistoryRepository extends JpaRepository<TicketHistory, UUID> {

    @EntityGraph(attributePaths = "changedBy")
    List<TicketHistory> findByTicketIdOrderByCreatedAtAscIdAsc(UUID ticketId);
}
