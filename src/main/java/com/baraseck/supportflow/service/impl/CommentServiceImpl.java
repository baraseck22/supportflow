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
import com.baraseck.supportflow.service.CommentService;
import com.baraseck.supportflow.security.CurrentUserService;
import com.baraseck.supportflow.security.TicketAccessPolicy;
import com.baraseck.supportflow.observability.SupportFlowMetrics;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {
    private static final EnumSet<Role> INTERNAL_NOTE_ROLES =
            EnumSet.of(Role.SUPPORT_N1, Role.SUPPORT_N2, Role.ADMIN);

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final TicketHistoryRepository historyRepository;
    private final CommentMapper commentMapper;
    private final CurrentUserService currentUserService;
    private final SupportFlowMetrics metrics;

    @Override
    @Transactional
    public CommentResponse addComment(UUID ticketId, AddCommentRequest request) {
        Ticket ticket = requireTicket(ticketId);
        User author = currentUserService.requireUser();
        TicketAccessPolicy.requireRead(ticket, author);
        if (ticket.getStatus() == TicketStatus.CLOSED) {
            throw new BusinessRuleException("Un ticket fermé ne peut plus recevoir de commentaire");
        }
        validateContent(request.content());
        if (request.internal() && !INTERNAL_NOTE_ROLES.contains(author.getRole())) {
            throw new BusinessRuleException("Une note interne est réservée à l'équipe support");
        }

        Comment comment = new Comment();
        comment.setTicket(ticket);
        comment.setAuthor(author);
        comment.setContent(request.content().trim());
        comment.setInternal(request.internal());
        Comment saved = commentRepository.save(comment);

        TicketHistory history = new TicketHistory();
        history.setTicket(ticket);
        history.setChangedBy(author);
        history.setFieldName("comment");
        history.setOldValue(null);
        history.setNewValue(request.internal() ? "INTERNAL_NOTE_ADDED" : "PUBLIC_COMMENT_ADDED");
        historyRepository.save(history);
        CommentResponse response = commentMapper.toResponse(saved);
        metrics.commentAdded();
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentResponse> getComments(UUID ticketId) {
        Ticket ticket = requireTicket(ticketId);
        User current = currentUserService.requireUser();
        TicketAccessPolicy.requireRead(ticket, current);
        return commentRepository.findByTicketIdOrderByCreatedAtAscIdAsc(ticketId).stream()
                .filter(comment -> !comment.isInternal() || TicketAccessPolicy.isSupport(current))
                .map(commentMapper::toResponse)
                .toList();
    }

    private Ticket requireTicket(UUID ticketId) {
        return ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket introuvable"));
    }

    private User requireActiveUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
        if (!user.isActive()) throw new BusinessRuleException("L'utilisateur est inactif");
        return user;
    }

    private void validateContent(String content) {
        if (content == null || content.isBlank()) {
            throw new BusinessRuleException("Le contenu du commentaire est obligatoire");
        }
        if (content.length() > 5000) {
            throw new BusinessRuleException("Le contenu du commentaire est limité à 5000 caractères");
        }
    }
}
