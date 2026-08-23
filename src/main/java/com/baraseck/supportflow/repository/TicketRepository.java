package com.baraseck.supportflow.repository;

import com.baraseck.supportflow.entity.Ticket;
import com.baraseck.supportflow.entity.TicketPriority;
import com.baraseck.supportflow.entity.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.Optional;
import java.util.UUID;
import java.time.Instant;
import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, UUID>, JpaSpecificationExecutor<Ticket> {
    Optional<Ticket> findByTicketNumber(String ticketNumber);

    Page<Ticket> findByStatus(TicketStatus status, Pageable pageable);
    Page<Ticket> findByPriority(TicketPriority priority, Pageable pageable);
    Page<Ticket> findByAssignedToId(UUID assignedToId, Pageable pageable);
    Page<Ticket> findByCreatedById(UUID createdById, Pageable pageable);

    long countByStatus(TicketStatus status);

    @Query("select count(t) from Ticket t where t.status not in (com.baraseck.supportflow.entity.TicketStatus.RESOLVED, com.baraseck.supportflow.entity.TicketStatus.CLOSED)")
    long countOpen();

    @Query("select count(t) from Ticket t where t.priority = com.baraseck.supportflow.entity.TicketPriority.CRITICAL and t.status not in (com.baraseck.supportflow.entity.TicketStatus.RESOLVED, com.baraseck.supportflow.entity.TicketStatus.CLOSED)")
    long countCriticalOpen();

    @Query("select count(t) from Ticket t where t.assignedTo is null and t.status not in (com.baraseck.supportflow.entity.TicketStatus.RESOLVED, com.baraseck.supportflow.entity.TicketStatus.CLOSED)")
    long countUnassignedOpen();

    @Query("select count(t) from Ticket t where t.firstResponseAt is null and t.status not in (com.baraseck.supportflow.entity.TicketStatus.RESOLVED, com.baraseck.supportflow.entity.TicketStatus.CLOSED) and t.responseDueAt < :now")
    long countResponseSlaBreached(Instant now);

    @Query("select count(t) from Ticket t where t.firstResponseAt is null and t.status not in (com.baraseck.supportflow.entity.TicketStatus.RESOLVED, com.baraseck.supportflow.entity.TicketStatus.CLOSED) and t.responseDueAt >= :now and timestampdiff(second, :now, t.responseDueAt) <= timestampdiff(second, t.createdAt, t.responseDueAt) * 0.20")
    long countResponseSlaAtRisk(Instant now);

    @Query("select count(t) from Ticket t where t.resolvedAt is null and t.status not in (com.baraseck.supportflow.entity.TicketStatus.RESOLVED, com.baraseck.supportflow.entity.TicketStatus.CLOSED) and t.resolutionDueAt < :now")
    long countResolutionSlaBreached(Instant now);

    @Query("select count(t) from Ticket t where t.resolvedAt is null and t.status not in (com.baraseck.supportflow.entity.TicketStatus.RESOLVED, com.baraseck.supportflow.entity.TicketStatus.CLOSED) and t.resolutionDueAt >= :now and timestampdiff(second, :now, t.resolutionDueAt) <= timestampdiff(second, t.createdAt, t.resolutionDueAt) * 0.20")
    long countResolutionSlaAtRisk(Instant now);

    @EntityGraph(attributePaths = "assignedTo")
    @Query("select t from Ticket t order by t.createdAt desc, t.id desc")
    List<Ticket> findRecentTickets(Pageable pageable);

    @EntityGraph(attributePaths = "assignedTo")
    @Query("select t from Ticket t where t.status not in (com.baraseck.supportflow.entity.TicketStatus.RESOLVED, com.baraseck.supportflow.entity.TicketStatus.CLOSED) order by case when ((t.firstResponseAt is null and t.responseDueAt < :now) or (t.resolvedAt is null and t.resolutionDueAt < :now)) then 0 else 1 end, case when t.priority = com.baraseck.supportflow.entity.TicketPriority.CRITICAL then 0 else 1 end, case when t.assignedTo is null then 0 else 1 end, t.resolutionDueAt asc, t.createdAt asc")
    List<Ticket> findPriorityTickets(Instant now, Pageable pageable);

    @Query(value = "SELECT nextval('ticket_number_seq')", nativeQuery = true)
    long nextTicketNumberSequenceValue();
}
