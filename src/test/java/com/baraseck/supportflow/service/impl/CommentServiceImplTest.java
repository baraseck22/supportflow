package com.baraseck.supportflow.service.impl;

import com.baraseck.supportflow.dto.AddCommentRequest;
import com.baraseck.supportflow.dto.CommentResponse;
import com.baraseck.supportflow.entity.Comment;
import com.baraseck.supportflow.entity.Role;
import com.baraseck.supportflow.entity.Ticket;
import com.baraseck.supportflow.entity.TicketHistory;
import com.baraseck.supportflow.entity.TicketStatus;
import com.baraseck.supportflow.entity.User;
import com.baraseck.supportflow.exception.BusinessRuleException;
import com.baraseck.supportflow.exception.ResourceNotFoundException;
import com.baraseck.supportflow.mapper.CommentMapper;
import com.baraseck.supportflow.repository.CommentRepository;
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
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CommentServiceImplTest {
    @Mock private TicketRepository ticketRepository;
    @Mock private UserRepository userRepository;
    @Mock private CommentRepository commentRepository;
    @Mock private TicketHistoryRepository historyRepository;
    @Mock private CommentMapper commentMapper;
    @Mock private CurrentUserService currentUserService;
    @Mock private SupportFlowMetrics metrics;
    @InjectMocks private CommentServiceImpl service;

    @BeforeEach
    void authenticatedSupportByDefault() {
        when(currentUserService.requireUser()).thenReturn(user(UUID.randomUUID(), Role.SUPPORT_N1, true));
    }

    @Test
    void userAddsPublicComment() {
        Prepared prepared = prepare(Role.USER, true, TicketStatus.IN_PROGRESS, false);
        assertThatCode(() -> service.addComment(prepared.ticketId(), prepared.request())).doesNotThrowAnyException();
        verify(commentRepository).save(any(Comment.class));
        verify(metrics).commentAdded();
    }

    @Test
    void supportN1AddsPublicComment() {
        Prepared prepared = prepare(Role.SUPPORT_N1, true, TicketStatus.IN_PROGRESS, false);
        assertThatCode(() -> service.addComment(prepared.ticketId(), prepared.request())).doesNotThrowAnyException();
    }

    @Test
    void supportN1AddsInternalNote() {
        Prepared prepared = prepare(Role.SUPPORT_N1, true, TicketStatus.IN_PROGRESS, true);
        assertThatCode(() -> service.addComment(prepared.ticketId(), prepared.request())).doesNotThrowAnyException();
    }

    @Test
    void supportN2AddsInternalNote() {
        Prepared prepared = prepare(Role.SUPPORT_N2, true, TicketStatus.IN_PROGRESS, true);
        assertThatCode(() -> service.addComment(prepared.ticketId(), prepared.request())).doesNotThrowAnyException();
    }

    @Test
    void userCannotAddInternalNote() {
        Prepared prepared = prepare(Role.USER, true, TicketStatus.IN_PROGRESS, true);
        assertThatThrownBy(() -> service.addComment(prepared.ticketId(), prepared.request()))
                .isInstanceOf(BusinessRuleException.class);
        verify(commentRepository, never()).save(any());
        verify(metrics, never()).commentAdded();
    }

    @Test
    void inactiveUserIsRejected() {
        Prepared prepared = prepare(Role.SUPPORT_N1, false, TicketStatus.IN_PROGRESS, false);
        assertThatThrownBy(() -> service.addComment(prepared.ticketId(), prepared.request()))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void missingTicketIsRejected() {
        UUID ticketId = UUID.randomUUID();
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.empty());
        AddCommentRequest request = new AddCommentRequest(UUID.randomUUID(), "Message", false);
        assertThatThrownBy(() -> service.addComment(ticketId, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void closedTicketRejectsComment() {
        UUID ticketId = UUID.randomUUID();
        Ticket ticket = ticket(TicketStatus.CLOSED);
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        AddCommentRequest request = new AddCommentRequest(UUID.randomUUID(), "Message", false);
        assertThatThrownBy(() -> service.addComment(ticketId, request))
                .isInstanceOf(BusinessRuleException.class);
        verify(userRepository, never()).findById(any());
        verify(metrics, never()).commentAdded();
    }

    @Test
    void publicCommentCreatesLightweightHistory() {
        Prepared prepared = prepare(Role.USER, true, TicketStatus.IN_PROGRESS, false);
        service.addComment(prepared.ticketId(), prepared.request());
        TicketHistory history = captureHistory();
        assertThat(history.getFieldName()).isEqualTo("comment");
        assertThat(history.getOldValue()).isNull();
        assertThat(history.getNewValue()).isEqualTo("PUBLIC_COMMENT_ADDED");
    }

    @Test
    void internalNoteCreatesLightweightHistory() {
        Prepared prepared = prepare(Role.SUPPORT_N1, true, TicketStatus.IN_PROGRESS, true);
        service.addComment(prepared.ticketId(), prepared.request());
        assertThat(captureHistory().getNewValue()).isEqualTo("INTERNAL_NOTE_ADDED");
    }

    @Test
    void commentContentIsNeverCopiedToHistory() {
        Prepared prepared = prepare(Role.USER, true, TicketStatus.IN_PROGRESS, false);
        service.addComment(prepared.ticketId(), prepared.request());
        TicketHistory history = captureHistory();
        assertThat(List.of(history.getFieldName(), history.getNewValue()))
                .doesNotContain(prepared.request().content());
    }

    @Test
    void getCommentsPreservesRepositoryChronologicalOrder() {
        UUID ticketId = UUID.randomUUID();
        Comment first = new Comment();
        Comment second = new Comment();
        CommentResponse firstResponse = response("Premier", Instant.parse("2026-08-20T10:00:00Z"));
        CommentResponse secondResponse = response("Second", Instant.parse("2026-08-20T11:00:00Z"));
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket(TicketStatus.IN_PROGRESS)));
        when(commentRepository.findByTicketIdOrderByCreatedAtAscIdAsc(ticketId)).thenReturn(List.of(first, second));
        when(commentMapper.toResponse(first)).thenReturn(firstResponse);
        when(commentMapper.toResponse(second)).thenReturn(secondResponse);

        assertThat(service.getComments(ticketId)).containsExactly(firstResponse, secondResponse);
    }

    private Prepared prepare(Role role, boolean active, TicketStatus status, boolean internal) {
        UUID ticketId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        Ticket ticket = ticket(status);
        User author = user(authorId, role, active);
        ticket.setCreatedBy(author);
        AddCommentRequest request = new AddCommentRequest(authorId, "Contenu de diagnostic", internal);
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        if (active) when(currentUserService.requireUser()).thenReturn(author);
        else when(currentUserService.requireUser()).thenThrow(new BusinessRuleException("L'utilisateur est inactif"));
        if (active && (!internal || role != Role.USER)) {
            when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        }
        return new Prepared(ticketId, request);
    }

    private TicketHistory captureHistory() {
        ArgumentCaptor<TicketHistory> captor = ArgumentCaptor.forClass(TicketHistory.class);
        verify(historyRepository).save(captor.capture());
        return captor.getValue();
    }

    private Ticket ticket(TicketStatus status) {
        Ticket ticket = new Ticket();
        ticket.setStatus(status);
        return ticket;
    }

    private User user(UUID id, Role role, boolean active) {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", id);
        user.setRole(role);
        user.setActive(active);
        return user;
    }

    private CommentResponse response(String content, Instant createdAt) {
        return new CommentResponse(UUID.randomUUID(), content, false, null, createdAt, createdAt);
    }

    private record Prepared(UUID ticketId, AddCommentRequest request) {}
}
