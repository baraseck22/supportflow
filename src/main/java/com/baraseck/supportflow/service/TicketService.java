package com.baraseck.supportflow.service;

import com.baraseck.supportflow.dto.AssignTicketRequest;
import com.baraseck.supportflow.dto.ChangeTicketStatusRequest;
import com.baraseck.supportflow.dto.CreateTicketRequest;
import com.baraseck.supportflow.dto.EscalateTicketRequest;
import com.baraseck.supportflow.dto.TicketResponse;
import com.baraseck.supportflow.dto.TicketHistoryResponse;
import com.baraseck.supportflow.entity.TicketPriority;
import com.baraseck.supportflow.entity.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;
import java.util.List;

public interface TicketService {
    TicketResponse createTicket(CreateTicketRequest request);
    TicketResponse assignTicket(UUID ticketId, AssignTicketRequest request);
    TicketResponse changeStatus(UUID ticketId, ChangeTicketStatusRequest request);
    TicketResponse escalateTicket(UUID ticketId, EscalateTicketRequest request);
    TicketResponse getTicketById(UUID id);
    TicketResponse getTicketByNumber(String ticketNumber);
    Page<TicketResponse> findAll(Pageable pageable);
    Page<TicketResponse> findByStatus(TicketStatus status, Pageable pageable);
    Page<TicketResponse> findByPriority(TicketPriority priority, Pageable pageable);
    Page<TicketResponse> findByAssignedTo(UUID assignedToId, Pageable pageable);
    Page<TicketResponse> findByCreatedBy(UUID createdById, Pageable pageable);
    Page<TicketResponse> findTickets(String search, TicketStatus status, TicketPriority priority,
                                     UUID assignedToId, boolean unassigned, UUID createdById,
                                     Pageable pageable);
    List<TicketHistoryResponse> getTicketHistory(UUID ticketId);
}
