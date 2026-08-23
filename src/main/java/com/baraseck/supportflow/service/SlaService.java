package com.baraseck.supportflow.service;

import com.baraseck.supportflow.dto.SlaSummaryResponse;
import com.baraseck.supportflow.entity.SlaStatus;
import com.baraseck.supportflow.entity.Ticket;
import com.baraseck.supportflow.exception.ResourceNotFoundException;
import com.baraseck.supportflow.repository.TicketRepository;
import com.baraseck.supportflow.security.CurrentUserService;
import com.baraseck.supportflow.security.TicketAccessPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SlaService {
    private static final double AT_RISK_REMAINING_RATIO = 0.20;

    private final TicketRepository ticketRepository;
    private final Clock clock;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public SlaSummaryResponse getTicketSla(UUID ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket introuvable"));
        TicketAccessPolicy.requireRead(ticket, currentUserService.requireUser());
        Instant now = clock.instant();
        return new SlaSummaryResponse(
                ticket.getResponseDueAt(), ticket.getResolutionDueAt(), ticket.getFirstResponseAt(),
                ticket.getResolvedAt(),
                status(ticket.getCreatedAt(), ticket.getResponseDueAt(), ticket.getFirstResponseAt(), now),
                status(ticket.getCreatedAt(), ticket.getResolutionDueAt(), ticket.getResolvedAt(), now),
                remainingSeconds(ticket.getResponseDueAt(), ticket.getFirstResponseAt(), now),
                remainingSeconds(ticket.getResolutionDueAt(), ticket.getResolvedAt(), now));
    }

    SlaStatus status(Instant startedAt, Instant dueAt, Instant completedAt, Instant now) {
        if (completedAt != null) {
            return completedAt.isAfter(dueAt) ? SlaStatus.BREACHED : SlaStatus.COMPLETED;
        }
        if (now.isAfter(dueAt)) return SlaStatus.BREACHED;
        Duration total = Duration.between(startedAt, dueAt);
        Duration remaining = Duration.between(now, dueAt);
        return remaining.toMillis() <= total.toMillis() * AT_RISK_REMAINING_RATIO
                ? SlaStatus.AT_RISK : SlaStatus.ON_TIME;
    }

    private long remainingSeconds(Instant dueAt, Instant completedAt, Instant now) {
        if (completedAt != null || !now.isBefore(dueAt)) return 0L;
        return Duration.between(now, dueAt).getSeconds();
    }
}
