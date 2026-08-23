package com.baraseck.supportflow.service.impl;

import com.baraseck.supportflow.dto.AssignTicketRequest;
import com.baraseck.supportflow.dto.ChangeTicketStatusRequest;
import com.baraseck.supportflow.dto.CreateTicketRequest;
import com.baraseck.supportflow.dto.EscalateTicketRequest;
import com.baraseck.supportflow.dto.TicketHistoryResponse;
import com.baraseck.supportflow.entity.Category;
import com.baraseck.supportflow.entity.Role;
import com.baraseck.supportflow.entity.Ticket;
import com.baraseck.supportflow.entity.TicketHistory;
import com.baraseck.supportflow.entity.TicketPriority;
import com.baraseck.supportflow.entity.TicketStatus;
import com.baraseck.supportflow.entity.User;
import com.baraseck.supportflow.exception.BusinessRuleException;
import com.baraseck.supportflow.exception.InvalidTicketTransitionException;
import com.baraseck.supportflow.exception.ResourceNotFoundException;
import com.baraseck.supportflow.mapper.TicketMapper;
import com.baraseck.supportflow.mapper.TicketHistoryMapper;
import com.baraseck.supportflow.service.SlaPolicyService;
import com.baraseck.supportflow.repository.CategoryRepository;
import com.baraseck.supportflow.repository.TicketHistoryRepository;
import com.baraseck.supportflow.repository.TicketRepository;
import com.baraseck.supportflow.repository.UserRepository;
import com.baraseck.supportflow.security.CurrentUserService;
import com.baraseck.supportflow.observability.SupportFlowMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TicketServiceImplTest {

    @Mock private TicketRepository ticketRepository;
    @Mock private UserRepository userRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private TicketHistoryRepository historyRepository;
    @Mock private TicketMapper ticketMapper;
    @Mock private TicketHistoryMapper ticketHistoryMapper;
    @Mock private CurrentUserService currentUserService;
    @Mock private SupportFlowMetrics metrics;
    @Spy private SlaPolicyService slaPolicyService = new SlaPolicyService();
    @Spy private Clock clock = Clock.fixed(Instant.parse("2026-08-21T10:00:00Z"), ZoneOffset.UTC);
    @InjectMocks private TicketServiceImpl service;

    @BeforeEach
    void authenticatedAdminByDefault() {
        when(currentUserService.requireUser()).thenReturn(user(UUID.randomUUID(), Role.ADMIN, true));
    }

    @Test
    void createsTicketWithInitialStateAndHistory() {
        UUID creatorId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        User creator = user(creatorId, Role.USER, true);
        when(currentUserService.requireUser()).thenReturn(creator);
        Category category = activeCategory();
        when(userRepository.findById(creatorId)).thenReturn(Optional.of(creator));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(ticketRepository.nextTicketNumberSequenceValue()).thenReturn(1L);
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.createTicket(new CreateTicketRequest(" Incident ", " Description ",
                TicketPriority.HIGH, categoryId, creatorId));

        ArgumentCaptor<Ticket> ticketCaptor = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketRepository).save(ticketCaptor.capture());
        Ticket ticket = ticketCaptor.getValue();
        assertThat(ticket.getTicketNumber()).isEqualTo("INC-000001");
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.NEW);
        assertThat(ticket.getAssignedTo()).isNull();
        assertThat(ticket.getResolvedAt()).isNull();
        assertThat(ticket.getClosedAt()).isNull();
        verify(historyRepository).save(any(TicketHistory.class));
        verify(metrics).ticketCreated();
    }

    @Test
    void refusesCreationWhenUserDoesNotExist() {
        UUID creatorId = UUID.randomUUID();
        when(currentUserService.requireUser()).thenThrow(new ResourceNotFoundException("Utilisateur introuvable"));
        CreateTicketRequest request = new CreateTicketRequest("Incident", "Description",
                TicketPriority.MEDIUM, UUID.randomUUID(), creatorId);

        assertThatThrownBy(() -> service.createTicket(request)).isInstanceOf(ResourceNotFoundException.class);
        verify(ticketRepository, never()).save(any());
        verify(metrics, never()).ticketCreated();
    }

    @Test
    void refusesCreationWithInactiveCategory() {
        UUID creatorId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        Category category = new Category();
        category.setActive(false);
        when(userRepository.findById(creatorId)).thenReturn(Optional.of(user(creatorId, Role.USER, true)));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));

        assertThatThrownBy(() -> service.createTicket(new CreateTicketRequest("Incident", "Description",
                TicketPriority.LOW, categoryId, creatorId))).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void formatsTicketNumberFromDatabaseSequence() {
        UUID creatorId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        when(userRepository.findById(creatorId)).thenReturn(Optional.of(user(creatorId, Role.USER, true)));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(activeCategory()));
        when(ticketRepository.nextTicketNumberSequenceValue()).thenReturn(42L);
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.createTicket(new CreateTicketRequest("Incident", "Description",
                TicketPriority.LOW, categoryId, creatorId));

        ArgumentCaptor<Ticket> captor = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketRepository).save(captor.capture());
        assertThat(captor.getValue().getTicketNumber()).isEqualTo("INC-000042");
    }

    @Test
    void assignsTicketToSupportN1AndStartsNewTicket() {
        UUID ticketId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID assigneeId = UUID.randomUUID();
        Ticket ticket = ticket(TicketStatus.NEW);
        User actor = user(actorId, Role.ADMIN, true);
        User assignee = user(assigneeId, Role.SUPPORT_N1, true);
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(userRepository.findById(actorId)).thenReturn(Optional.of(actor));
        when(userRepository.findById(assigneeId)).thenReturn(Optional.of(assignee));
        when(ticketRepository.save(ticket)).thenReturn(ticket);

        service.assignTicket(ticketId, new AssignTicketRequest(assigneeId, actorId));

        assertThat(ticket.getAssignedTo()).isSameAs(assignee);
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
        ArgumentCaptor<TicketHistory> history = ArgumentCaptor.forClass(TicketHistory.class);
        verify(historyRepository, org.mockito.Mockito.times(3)).save(history.capture());
        assertThat(history.getAllValues()).extracting(TicketHistory::getFieldName)
                .containsExactly("assignedTo", "sla", "status");
        assertThat(ticket.getFirstResponseAt()).isEqualTo(clock.instant());
    }

    @Test
    void refusesAssignmentToRegularUser() {
        UUID ticketId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID assigneeId = UUID.randomUUID();
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket(TicketStatus.NEW)));
        when(userRepository.findById(actorId)).thenReturn(Optional.of(user(actorId, Role.ADMIN, true)));
        when(userRepository.findById(assigneeId)).thenReturn(Optional.of(user(assigneeId, Role.USER, true)));

        assertThatThrownBy(() -> service.assignTicket(ticketId,
                new AssignTicketRequest(assigneeId, actorId))).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void secondAssignmentDoesNotChangeFirstResponseOrDuplicateSlaHistory() {
        UUID ticketId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID assigneeId = UUID.randomUUID();
        Instant firstResponse = Instant.parse("2026-08-21T09:00:00Z");
        Ticket ticket = ticket(TicketStatus.IN_PROGRESS);
        ticket.setFirstResponseAt(firstResponse);
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(userRepository.findById(actorId)).thenReturn(Optional.of(user(actorId, Role.ADMIN, true)));
        when(userRepository.findById(assigneeId)).thenReturn(Optional.of(user(assigneeId, Role.SUPPORT_N2, true)));
        when(ticketRepository.save(ticket)).thenReturn(ticket);

        service.assignTicket(ticketId, new AssignTicketRequest(assigneeId, actorId));

        assertThat(ticket.getFirstResponseAt()).isEqualTo(firstResponse);
        ArgumentCaptor<TicketHistory> history = ArgumentCaptor.forClass(TicketHistory.class);
        verify(historyRepository).save(history.capture());
        assertThat(history.getValue().getFieldName()).isEqualTo("assignedTo");
    }

    @ParameterizedTest
    @CsvSource({
            "CRITICAL,900,7200",
            "HIGH,1800,14400",
            "MEDIUM,7200,28800",
            "LOW,14400,86400"
    })
    void creationCalculatesSlaDeadlines(TicketPriority priority, long responseSeconds, long resolutionSeconds) {
        UUID creatorId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        User creator = user(creatorId, Role.USER, true);
        when(currentUserService.requireUser()).thenReturn(creator);
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(activeCategory()));
        when(ticketRepository.nextTicketNumberSequenceValue()).thenReturn(100L);
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.createTicket(new CreateTicketRequest("Incident SLA", "Description", priority, categoryId, creatorId));

        ArgumentCaptor<Ticket> ticket = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketRepository).save(ticket.capture());
        assertThat(ticket.getValue().getResponseDueAt()).isEqualTo(clock.instant().plusSeconds(responseSeconds));
        assertThat(ticket.getValue().getResolutionDueAt()).isEqualTo(clock.instant().plusSeconds(resolutionSeconds));
    }

    @Test
    void resolvesInProgressTicket() {
        Ticket ticket = changeStatus(TicketStatus.IN_PROGRESS, TicketStatus.RESOLVED);
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.RESOLVED);
        assertThat(ticket.getResolvedAt()).isNotNull();
        assertThat(ticket.getClosedAt()).isNull();
        verify(metrics).ticketResolved();
    }

    @Test
    void closesResolvedTicket() {
        Ticket ticket = ticket(TicketStatus.RESOLVED);
        java.time.Instant resolvedAt = java.time.Instant.parse("2026-08-20T10:00:00Z");
        ticket.setResolvedAt(resolvedAt);

        changeStatus(ticket, TicketStatus.CLOSED);

        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.CLOSED);
        assertThat(ticket.getResolvedAt()).isEqualTo(resolvedAt);
        assertThat(ticket.getClosedAt()).isNotNull();
    }

    @Test
    void reopeningResolvedTicketClearsResolvedAt() {
        Ticket ticket = ticket(TicketStatus.RESOLVED);
        ticket.setResolvedAt(java.time.Instant.now());
        changeStatus(ticket, TicketStatus.IN_PROGRESS);
        assertThat(ticket.getResolvedAt()).isNull();
        assertThat(ticket.getClosedAt()).isNull();
    }

    @Test
    void refusesTransitionFromClosedTicket() {
        assertInvalidTransition(TicketStatus.CLOSED, TicketStatus.IN_PROGRESS);
    }

    @Test
    void refusesInvalidTransition() {
        assertInvalidTransition(TicketStatus.NEW, TicketStatus.RESOLVED);
    }

    @Test
    void escalatesFromSupportN1ToSupportN2WithHistory() {
        UUID ticketId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        Ticket ticket = ticket(TicketStatus.IN_PROGRESS);
        User supportN1 = user(actorId, Role.SUPPORT_N1, true);
        User supportN2 = user(targetId, Role.SUPPORT_N2, true);
        ticket.setAssignedTo(supportN1);
        when(currentUserService.requireUser()).thenReturn(supportN1);
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(userRepository.findById(actorId)).thenReturn(Optional.of(supportN1));
        when(userRepository.findById(targetId)).thenReturn(Optional.of(supportN2));
        when(ticketRepository.save(ticket)).thenReturn(ticket);

        service.escalateTicket(ticketId, new EscalateTicketRequest(targetId, actorId, "Expertise N2 requise"));

        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.ESCALATED);
        assertThat(ticket.getAssignedTo()).isSameAs(supportN2);
        ArgumentCaptor<TicketHistory> history = ArgumentCaptor.forClass(TicketHistory.class);
        verify(historyRepository, org.mockito.Mockito.times(3)).save(history.capture());
        List<TicketHistory> histories = history.getAllValues();
        assertThat(histories).extracting(TicketHistory::getFieldName)
                .containsExactly("assignedTo", "status", "escalationReason");
        assertThat(histories).filteredOn(item -> item.getFieldName().equals("status"))
                .singleElement()
                .satisfies(item -> {
                    assertThat(item.getOldValue()).isEqualTo("IN_PROGRESS");
                    assertThat(item.getNewValue()).isEqualTo("ESCALATED");
                });
        assertThat(histories).filteredOn(item -> item.getFieldName().equals("assignedTo"))
                .singleElement()
                .satisfies(item -> {
                    assertThat(item.getOldValue()).isEqualTo(actorId.toString());
                    assertThat(item.getNewValue()).isEqualTo(targetId.toString());
                });
        assertThat(histories).filteredOn(item -> item.getFieldName().equals("escalationReason"))
                .singleElement();
        verify(metrics).ticketEscalated();
    }

    @Test
    void regularUserCannotEscalate() {
        UUID ticketId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket(TicketStatus.IN_PROGRESS)));
        when(currentUserService.requireUser()).thenReturn(user(actorId, Role.USER, true));

        assertThatThrownBy(() -> service.escalateTicket(ticketId,
                new EscalateTicketRequest(null, actorId, "Besoin d'aide")))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void escalationTargetMustBeSupportN2OrAdmin() {
        UUID ticketId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket(TicketStatus.IN_PROGRESS)));
        when(currentUserService.requireUser()).thenReturn(user(actorId, Role.SUPPORT_N1, true));
        when(userRepository.findById(targetId)).thenReturn(Optional.of(user(targetId, Role.SUPPORT_N1, true)));

        assertThatThrownBy(() -> service.escalateTicket(ticketId,
                new EscalateTicketRequest(targetId, actorId, "Besoin N2")))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void returnsTicketHistoryInRepositoryChronologicalOrder() {
        UUID ticketId = UUID.randomUUID();
        TicketHistory first = new TicketHistory();
        TicketHistory second = new TicketHistory();
        TicketHistoryResponse firstResponse = new TicketHistoryResponse(
                UUID.randomUUID(), "status", "NEW", "IN_PROGRESS", null,
                java.time.Instant.parse("2026-08-20T10:00:00Z"));
        TicketHistoryResponse secondResponse = new TicketHistoryResponse(
                UUID.randomUUID(), "status", "IN_PROGRESS", "RESOLVED", null,
                java.time.Instant.parse("2026-08-20T11:00:00Z"));
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket(TicketStatus.RESOLVED)));
        when(historyRepository.findByTicketIdOrderByCreatedAtAscIdAsc(ticketId))
                .thenReturn(List.of(first, second));
        when(ticketHistoryMapper.toResponse(first)).thenReturn(firstResponse);
        when(ticketHistoryMapper.toResponse(second)).thenReturn(secondResponse);

        List<TicketHistoryResponse> result = service.getTicketHistory(ticketId);

        assertThat(result).containsExactly(firstResponse, secondResponse);
        verify(historyRepository).findByTicketIdOrderByCreatedAtAscIdAsc(ticketId);
    }

    @Test
    void historyFailsWhenTicketDoesNotExist() {
        UUID ticketId = UUID.randomUUID();
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getTicketHistory(ticketId))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(historyRepository, never()).findByTicketIdOrderByCreatedAtAscIdAsc(ticketId);
    }

    private Ticket changeStatus(TicketStatus from, TicketStatus to) {
        Ticket ticket = ticket(from);
        return changeStatus(ticket, to);
    }

    private Ticket changeStatus(Ticket ticket, TicketStatus to) {
        UUID ticketId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        TicketStatus from = ticket.getStatus();
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(userRepository.findById(actorId)).thenReturn(Optional.of(user(actorId, Role.ADMIN, true)));
        when(ticketRepository.save(ticket)).thenReturn(ticket);
        service.changeStatus(ticketId, new ChangeTicketStatusRequest(to, actorId));
        ArgumentCaptor<TicketHistory> historyCaptor = ArgumentCaptor.forClass(TicketHistory.class);
        verify(historyRepository).save(historyCaptor.capture());
        TicketHistory history = historyCaptor.getValue();
        assertThat(history.getFieldName()).isEqualTo("status");
        assertThat(history.getOldValue()).isEqualTo(from.name());
        assertThat(history.getNewValue()).isEqualTo(to.name());
        return ticket;
    }

    private void assertInvalidTransition(TicketStatus from, TicketStatus to) {
        UUID ticketId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket(from)));
        when(userRepository.findById(actorId)).thenReturn(Optional.of(user(actorId, Role.ADMIN, true)));
        assertThatThrownBy(() -> service.changeStatus(ticketId, new ChangeTicketStatusRequest(to, actorId)))
                .isInstanceOf(InvalidTicketTransitionException.class);
        verify(historyRepository, never()).save(any(TicketHistory.class));
    }

    private Ticket ticket(TicketStatus status) {
        Ticket ticket = new Ticket();
        ticket.setStatus(status);
        return ticket;
    }

    private Category activeCategory() {
        Category category = new Category();
        category.setName("APPLICATION_ERROR");
        category.setActive(true);
        return category;
    }

    private User user(UUID id, Role role, boolean active) {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", id);
        user.setRole(role);
        user.setActive(active);
        return user;
    }
}
