package com.baraseck.supportflow.service.impl;

import com.baraseck.supportflow.dto.AssignTicketRequest;
import com.baraseck.supportflow.dto.ChangeTicketStatusRequest;
import com.baraseck.supportflow.dto.CreateTicketRequest;
import com.baraseck.supportflow.dto.EscalateTicketRequest;
import com.baraseck.supportflow.dto.TicketResponse;
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
import com.baraseck.supportflow.repository.CategoryRepository;
import com.baraseck.supportflow.repository.TicketHistoryRepository;
import com.baraseck.supportflow.repository.TicketRepository;
import com.baraseck.supportflow.repository.UserRepository;
import com.baraseck.supportflow.service.TicketService;
import com.baraseck.supportflow.service.SlaPolicyService;
import com.baraseck.supportflow.security.CurrentUserService;
import com.baraseck.supportflow.security.TicketAccessPolicy;
import com.baraseck.supportflow.observability.SupportFlowMetrics;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.Clock;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.baraseck.supportflow.repository.TicketSpecifications.*;

@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {

    private static final Map<TicketStatus, Set<TicketStatus>> ALLOWED_TRANSITIONS = transitions();

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final TicketHistoryRepository historyRepository;
    private final TicketMapper ticketMapper;
    private final TicketHistoryMapper ticketHistoryMapper;
    private final SlaPolicyService slaPolicyService;
    private final Clock clock;
    private final CurrentUserService currentUserService;
    private final SupportFlowMetrics metrics;

    @Override
    @Transactional
    public TicketResponse createTicket(CreateTicketRequest request) {
        validateCreateRequest(request);
        User createdBy = currentUserService.requireUser();
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Catégorie introuvable"));
        if (!category.isActive()) {
            throw new BusinessRuleException("La catégorie sélectionnée est inactive");
        }

        Ticket ticket = new Ticket();
        ticket.setTicketNumber(formatTicketNumber(ticketRepository.nextTicketNumberSequenceValue()));
        ticket.setTitle(request.title().trim());
        ticket.setDescription(request.description().trim());
        ticket.setPriority(request.priority());
        ticket.setStatus(TicketStatus.NEW);
        ticket.setCreatedBy(createdBy);
        ticket.setCategory(category);
        Instant createdAt = clock.instant();
        ticket.initializeTimestamps(createdAt);
        ticket.setResponseDueAt(createdAt.plus(slaPolicyService.getResponseTarget(request.priority())));
        ticket.setResolutionDueAt(createdAt.plus(slaPolicyService.getResolutionTarget(request.priority())));
        Ticket saved = ticketRepository.save(ticket);
        saveHistory(saved, createdBy, "ticket", null, saved.getTicketNumber());
        metrics.ticketCreated();
        return ticketMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public TicketResponse assignTicket(UUID ticketId, AssignTicketRequest request) {
        Ticket ticket = requireTicket(ticketId);
        User changedBy = currentUserService.requireUser();
        User assignedTo = requireActiveUser(request.assignedToUserId());
        if (!EnumSet.of(Role.SUPPORT_N1, Role.SUPPORT_N2, Role.ADMIN).contains(assignedTo.getRole())) {
            throw new BusinessRuleException("Un ticket ne peut être affecté qu'à un membre du support ou un administrateur");
        }

        String oldAssignee = userId(ticket.getAssignedTo());
        ticket.setAssignedTo(assignedTo);
        saveHistory(ticket, changedBy, "assignedTo", oldAssignee, userId(assignedTo));
        if (ticket.getFirstResponseAt() == null) {
            ticket.setFirstResponseAt(clock.instant());
            saveHistory(ticket, changedBy, "sla", null, "FIRST_RESPONSE_RECORDED");
        }
        if (ticket.getStatus() == TicketStatus.NEW) {
            TicketStatus oldStatus = ticket.getStatus();
            ticket.setStatus(TicketStatus.IN_PROGRESS);
            saveHistory(ticket, changedBy, "status", oldStatus.name(), ticket.getStatus().name());
        }
        return ticketMapper.toResponse(ticketRepository.save(ticket));
    }

    @Override
    @Transactional
    public TicketResponse changeStatus(UUID ticketId, ChangeTicketStatusRequest request) {
        Ticket ticket = requireTicket(ticketId);
        User changedBy = currentUserService.requireUser();
        applyStatusChange(ticket, request.status(), changedBy);
        TicketResponse response = ticketMapper.toResponse(ticketRepository.save(ticket));
        if (request.status() == TicketStatus.RESOLVED) metrics.ticketResolved();
        return response;
    }

    @Override
    @Transactional
    public TicketResponse escalateTicket(UUID ticketId, EscalateTicketRequest request) {
        if (request.reason() == null || request.reason().isBlank()) {
            throw new BusinessRuleException("La raison de l'escalade est obligatoire");
        }
        Ticket ticket = requireTicket(ticketId);
        User changedBy = currentUserService.requireUser();
        if (!EnumSet.of(Role.SUPPORT_N1, Role.ADMIN).contains(changedBy.getRole())) {
            throw new BusinessRuleException("Seul le support N1 ou un administrateur peut escalader un ticket");
        }
        if (request.targetUserId() != null) {
            User target = requireActiveUser(request.targetUserId());
            if (!EnumSet.of(Role.SUPPORT_N2, Role.ADMIN).contains(target.getRole())) {
                throw new BusinessRuleException("La cible d'une escalade doit être SUPPORT_N2 ou ADMIN");
            }
            String oldAssignee = userId(ticket.getAssignedTo());
            String newAssignee = userId(target);
            if (!java.util.Objects.equals(oldAssignee, newAssignee)) {
                ticket.setAssignedTo(target);
                saveHistory(ticket, changedBy, "assignedTo", oldAssignee, newAssignee);
            }
        }

        // Unique point de validation, mutation et traçage de la transition de statut.
        applyStatusChange(ticket, TicketStatus.ESCALATED, changedBy);
        saveHistory(ticket, changedBy, "escalationReason", null, request.reason().trim());
        TicketResponse response = ticketMapper.toResponse(ticketRepository.save(ticket));
        metrics.ticketEscalated();
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public TicketResponse getTicketById(UUID id) {
        Ticket ticket = requireTicket(id);
        TicketAccessPolicy.requireRead(ticket, currentUserService.requireUser());
        return ticketMapper.toResponse(ticket);
    }

    @Override
    @Transactional(readOnly = true)
    public TicketResponse getTicketByNumber(String ticketNumber) {
        Ticket ticket = ticketRepository.findByTicketNumber(ticketNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket introuvable"));
        TicketAccessPolicy.requireRead(ticket, currentUserService.requireUser());
        return ticketMapper.toResponse(ticket);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TicketResponse> findAll(Pageable pageable) {
        return ticketRepository.findAll(pageable).map(ticketMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TicketResponse> findByStatus(TicketStatus status, Pageable pageable) {
        return ticketRepository.findByStatus(status, pageable).map(ticketMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TicketResponse> findByPriority(TicketPriority priority, Pageable pageable) {
        return ticketRepository.findByPriority(priority, pageable).map(ticketMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TicketResponse> findByAssignedTo(UUID assignedToId, Pageable pageable) {
        return ticketRepository.findByAssignedToId(assignedToId, pageable).map(ticketMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TicketResponse> findByCreatedBy(UUID createdById, Pageable pageable) {
        return ticketRepository.findByCreatedById(createdById, pageable).map(ticketMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TicketResponse> findTickets(String search, TicketStatus status, TicketPriority priority,
                                            UUID assignedToId, boolean unassigned, UUID createdById,
                                            Pageable pageable) {
        User current = currentUserService.requireUser();
        if (assignedToId != null && unassigned) {
            throw new BusinessRuleException("Les filtres assignedTo et unassigned ne peuvent pas être combinés");
        }
        UUID effectiveCreatedBy = current.getRole() == Role.USER ? current.getId() : createdById;
        Specification<Ticket> filters = Specification.allOf(
                textContains(search), hasStatus(status), hasPriority(priority),
                assignedTo(assignedToId), isUnassigned(unassigned), createdBy(effectiveCreatedBy));
        return ticketRepository.findAll(filters, pageable).map(ticketMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketHistoryResponse> getTicketHistory(UUID ticketId) {
        Ticket ticket = requireTicket(ticketId);
        User current = currentUserService.requireUser();
        TicketAccessPolicy.requireRead(ticket, current);
        return historyRepository.findByTicketIdOrderByCreatedAtAscIdAsc(ticketId).stream()
                .filter(history -> current.getRole() != Role.USER
                        || !("comment".equals(history.getFieldName())
                        && "INTERNAL_NOTE_ADDED".equals(history.getNewValue())))
                .map(ticketHistoryMapper::toResponse)
                .toList();
    }

    private void applyStatusChange(Ticket ticket, TicketStatus newStatus, User changedBy) {
        TicketStatus oldStatus = ticket.getStatus();
        validateTransition(oldStatus, newStatus);
        if (oldStatus == TicketStatus.RESOLVED && newStatus == TicketStatus.IN_PROGRESS) {
            ticket.setResolvedAt(null);
        }
        if (newStatus == TicketStatus.RESOLVED) {
            ticket.setResolvedAt(Instant.now());
        }
        if (newStatus == TicketStatus.CLOSED) {
            ticket.setClosedAt(Instant.now());
        }
        ticket.setStatus(newStatus);
        saveHistory(ticket, changedBy, "status", oldStatus.name(), newStatus.name());
    }

    private void validateTransition(TicketStatus from, TicketStatus to) {
        if (!ALLOWED_TRANSITIONS.getOrDefault(from, Set.of()).contains(to)) {
            throw new InvalidTicketTransitionException(from, to);
        }
    }

    private Ticket requireTicket(UUID id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket introuvable"));
    }

    private User requireActiveUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
        if (!user.isActive()) {
            throw new BusinessRuleException("L'utilisateur est inactif");
        }
        return user;
    }

    private void saveHistory(Ticket ticket, User changedBy, String field, String oldValue, String newValue) {
        TicketHistory history = new TicketHistory();
        history.setTicket(ticket);
        history.setChangedBy(changedBy);
        history.setFieldName(field);
        history.setOldValue(oldValue);
        history.setNewValue(newValue);
        historyRepository.save(history);
    }

    private String formatTicketNumber(long sequence) {
        return "INC-%06d".formatted(sequence);
    }

    private void validateCreateRequest(CreateTicketRequest request) {
        if (request.title() == null || request.title().isBlank() || request.title().length() > 200) {
            throw new BusinessRuleException("Le titre est obligatoire et limité à 200 caractères");
        }
        if (request.description() == null || request.description().isBlank()) {
            throw new BusinessRuleException("La description est obligatoire");
        }
        if (request.priority() == null || request.categoryId() == null) {
            throw new BusinessRuleException("Les informations obligatoires du ticket sont manquantes");
        }
    }

    private String userId(User user) {
        return user == null ? null : user.getId().toString();
    }

    private static Map<TicketStatus, Set<TicketStatus>> transitions() {
        Map<TicketStatus, Set<TicketStatus>> transitions = new EnumMap<>(TicketStatus.class);
        transitions.put(TicketStatus.NEW, EnumSet.of(TicketStatus.IN_PROGRESS, TicketStatus.CLOSED));
        transitions.put(TicketStatus.IN_PROGRESS, EnumSet.of(TicketStatus.WAITING, TicketStatus.ESCALATED,
                TicketStatus.RESOLVED, TicketStatus.CLOSED));
        transitions.put(TicketStatus.WAITING, EnumSet.of(TicketStatus.IN_PROGRESS, TicketStatus.CLOSED));
        transitions.put(TicketStatus.ESCALATED, EnumSet.of(TicketStatus.IN_PROGRESS, TicketStatus.RESOLVED,
                TicketStatus.CLOSED));
        transitions.put(TicketStatus.RESOLVED, EnumSet.of(TicketStatus.IN_PROGRESS, TicketStatus.CLOSED));
        transitions.put(TicketStatus.CLOSED, EnumSet.noneOf(TicketStatus.class));
        return Map.copyOf(transitions);
    }
}
