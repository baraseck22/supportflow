package com.baraseck.supportflow.service;

import com.baraseck.supportflow.dto.SlaSummaryResponse;
import com.baraseck.supportflow.entity.SlaStatus;
import com.baraseck.supportflow.entity.Ticket;
import com.baraseck.supportflow.entity.TicketStatus;
import com.baraseck.supportflow.entity.User;
import com.baraseck.supportflow.entity.Role;
import com.baraseck.supportflow.security.CurrentUserService;
import com.baraseck.supportflow.repository.TicketRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SlaServiceTest {
    private static final Instant NOW = Instant.parse("2026-08-21T10:00:00Z");
    private static final Instant STARTED = Instant.parse("2026-08-21T08:00:00Z");
    @Mock private TicketRepository ticketRepository;
    @Mock private CurrentUserService currentUserService;

    @Test
    void responseBeforeRiskWindowIsOnTime() {
        assertThat(summary(ticket(STARTED, instant("12:00"), instant("20:00"))).responseStatus())
                .isEqualTo(SlaStatus.ON_TIME);
    }

    @Test
    void responseNearDeadlineIsAtRisk() {
        assertThat(summary(ticket(STARTED, instant("10:30"), instant("20:00"))).responseStatus())
                .isEqualTo(SlaStatus.AT_RISK);
    }

    @Test
    void unansweredResponsePastDeadlineIsBreached() {
        assertThat(summary(ticket(STARTED, instant("09:00"), instant("20:00"))).responseStatus())
                .isEqualTo(SlaStatus.BREACHED);
    }

    @Test
    void responseBeforeDeadlineIsCompleted() {
        Ticket ticket = ticket(STARTED, instant("10:00"), instant("20:00"));
        ticket.setFirstResponseAt(instant("09:30"));
        assertThat(summary(ticket).responseStatus()).isEqualTo(SlaStatus.COMPLETED);
    }

    @Test
    void responseAfterDeadlineIsBreached() {
        Ticket ticket = ticket(STARTED, instant("09:00"), instant("20:00"));
        ticket.setFirstResponseAt(instant("09:01"));
        assertThat(summary(ticket).responseStatus()).isEqualTo(SlaStatus.BREACHED);
    }

    @Test
    void resolutionBeforeRiskWindowIsOnTime() {
        assertThat(summary(ticket(STARTED, instant("12:00"), instant("18:00"))).resolutionStatus())
                .isEqualTo(SlaStatus.ON_TIME);
    }

    @Test
    void resolutionNearDeadlineIsAtRisk() {
        assertThat(summary(ticket(STARTED, instant("12:00"), instant("10:30"))).resolutionStatus())
                .isEqualTo(SlaStatus.AT_RISK);
    }

    @Test
    void unresolvedTicketPastDeadlineIsBreached() {
        assertThat(summary(ticket(STARTED, instant("12:00"), instant("09:00"))).resolutionStatus())
                .isEqualTo(SlaStatus.BREACHED);
    }

    @Test
    void resolutionBeforeDeadlineIsCompleted() {
        Ticket ticket = ticket(STARTED, instant("12:00"), instant("10:00"));
        ticket.setResolvedAt(instant("09:45"));
        assertThat(summary(ticket).resolutionStatus()).isEqualTo(SlaStatus.COMPLETED);
    }

    @Test
    void resolutionAfterDeadlineIsBreached() {
        Ticket ticket = ticket(STARTED, instant("12:00"), instant("09:00"));
        ticket.setResolvedAt(instant("09:01"));
        assertThat(summary(ticket).resolutionStatus()).isEqualTo(SlaStatus.BREACHED);
    }

    @Test
    void closedTicketUsesResolvedAtRatherThanClosedAt() {
        Ticket ticket = ticket(STARTED, instant("09:00"), instant("10:00"));
        ticket.setStatus(TicketStatus.CLOSED);
        ticket.setResolvedAt(instant("09:30"));
        ticket.setClosedAt(instant("11:00"));
        assertThat(summary(ticket).resolutionStatus()).isEqualTo(SlaStatus.COMPLETED);
    }

    private SlaSummaryResponse summary(Ticket ticket) {
        UUID id = UUID.randomUUID();
        when(ticketRepository.findById(id)).thenReturn(Optional.of(ticket));
        User support = new User();
        support.setRole(Role.SUPPORT_N1);
        when(currentUserService.requireUser()).thenReturn(support);
        return new SlaService(ticketRepository, Clock.fixed(NOW, ZoneOffset.UTC), currentUserService).getTicketSla(id);
    }

    private Ticket ticket(Instant createdAt, Instant responseDueAt, Instant resolutionDueAt) {
        Ticket ticket = new Ticket();
        ReflectionTestUtils.setField(ticket, "createdAt", createdAt);
        ticket.setResponseDueAt(responseDueAt);
        ticket.setResolutionDueAt(resolutionDueAt);
        ticket.setStatus(TicketStatus.IN_PROGRESS);
        return ticket;
    }

    private Instant instant(String time) {
        return Instant.parse("2026-08-21T" + time + ":00Z");
    }
}
