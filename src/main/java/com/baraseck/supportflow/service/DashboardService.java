package com.baraseck.supportflow.service;

import com.baraseck.supportflow.dto.DashboardSummaryResponse;
import com.baraseck.supportflow.dto.DashboardTicketSummary;
import com.baraseck.supportflow.entity.Ticket;
import com.baraseck.supportflow.entity.TicketStatus;
import com.baraseck.supportflow.mapper.UserSummaryMapper;
import com.baraseck.supportflow.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class DashboardService {
    private final TicketRepository ticketRepository;
    private final UserSummaryMapper userSummaryMapper;
    private final Clock clock;

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary() {
        Instant now = clock.instant();
        PageRequest topFive = PageRequest.of(0, 5);
        return new DashboardSummaryResponse(
                ticketRepository.countOpen(),
                ticketRepository.countByStatus(TicketStatus.NEW),
                ticketRepository.countByStatus(TicketStatus.IN_PROGRESS),
                ticketRepository.countByStatus(TicketStatus.WAITING),
                ticketRepository.countByStatus(TicketStatus.ESCALATED),
                ticketRepository.countCriticalOpen(),
                ticketRepository.countUnassignedOpen(),
                ticketRepository.countResponseSlaAtRisk(now),
                ticketRepository.countResponseSlaBreached(now),
                ticketRepository.countResolutionSlaAtRisk(now),
                ticketRepository.countResolutionSlaBreached(now),
                ticketRepository.findPriorityTickets(now, topFive).stream().map(this::toSummary).toList(),
                ticketRepository.findRecentTickets(topFive).stream().map(this::toSummary).toList());
    }

    private DashboardTicketSummary toSummary(Ticket ticket) {
        return new DashboardTicketSummary(ticket.getId(), ticket.getTicketNumber(), ticket.getTitle(),
                ticket.getStatus(), ticket.getPriority(), userSummaryMapper.toSummary(ticket.getAssignedTo()),
                ticket.getCreatedAt());
    }
}
