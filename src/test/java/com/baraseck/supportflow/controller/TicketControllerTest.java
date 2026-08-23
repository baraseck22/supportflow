package com.baraseck.supportflow.controller;

import com.baraseck.supportflow.dto.TicketResponse;
import com.baraseck.supportflow.dto.TicketHistoryResponse;
import com.baraseck.supportflow.dto.UserSummary;
import com.baraseck.supportflow.dto.CommentResponse;
import com.baraseck.supportflow.dto.SlaSummaryResponse;
import com.baraseck.supportflow.entity.Role;
import com.baraseck.supportflow.entity.SlaStatus;
import com.baraseck.supportflow.entity.TicketPriority;
import com.baraseck.supportflow.entity.TicketStatus;
import com.baraseck.supportflow.exception.InvalidTicketTransitionException;
import com.baraseck.supportflow.exception.ResourceNotFoundException;
import com.baraseck.supportflow.exception.BusinessRuleException;
import com.baraseck.supportflow.service.CommentService;
import com.baraseck.supportflow.service.SlaService;
import com.baraseck.supportflow.service.TicketService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import com.baraseck.supportflow.security.JwtService;
import com.baraseck.supportflow.security.DatabaseUserDetailsService;

import java.time.Instant;
import java.util.UUID;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TicketController.class)
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser(roles = "ADMIN")
class TicketControllerTest {

    private static final UUID TICKET_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final UUID USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID SUPPORT_ID = UUID.fromString("10000000-0000-0000-0000-000000000002");
    private static final UUID CATEGORY_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");

    @Autowired private MockMvc mockMvc;
    @MockitoBean private TicketService ticketService;
    @MockitoBean private CommentService commentService;
    @MockitoBean private SlaService slaService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private DatabaseUserDetailsService databaseUserDetailsService;

    @Test
    void postTicketReturnsCreated() throws Exception {
        when(ticketService.createTicket(any())).thenReturn(response(TicketStatus.NEW));

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ticketNumber").value("INC-000001"))
                .andExpect(jsonPath("$.status").value("NEW"));
    }

    @Test
    void invalidTicketReturnsBadRequestWithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.validationErrors.title").exists())
                .andExpect(jsonPath("$.validationErrors.categoryId").exists());
    }

    @Test
    void getTicketReturnsOk() throws Exception {
        when(ticketService.getTicketById(TICKET_ID)).thenReturn(response(TicketStatus.NEW));

        mockMvc.perform(get("/api/tickets/{id}", TICKET_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(TICKET_ID.toString()));
    }

    @Test
    void listForwardsCombinedSearchFiltersAndPagination() throws Exception {
        when(ticketService.findTickets(eq("paiement"), eq(TicketStatus.IN_PROGRESS),
                eq(TicketPriority.HIGH), eq(SUPPORT_ID), eq(false), eq(null), any()))
                .thenReturn(org.springframework.data.domain.Page.empty());

        mockMvc.perform(get("/api/tickets")
                        .param("search", "paiement")
                        .param("status", "IN_PROGRESS")
                        .param("priority", "HIGH")
                        .param("assignedTo", SUPPORT_ID.toString())
                        .param("page", "1").param("size", "10").param("sort", "createdAt,asc"))
                .andExpect(status().isOk());

        verify(ticketService).findTickets(eq("paiement"), eq(TicketStatus.IN_PROGRESS),
                eq(TicketPriority.HIGH), eq(SUPPORT_ID), eq(false), eq(null),
                org.mockito.ArgumentMatchers.argThat(pageable -> pageable.getPageNumber() == 1
                        && pageable.getPageSize() == 10
                        && pageable.getSort().getOrderFor("createdAt").isAscending()));
    }

    @Test
    void missingTicketReturnsNotFound() throws Exception {
        when(ticketService.getTicketById(TICKET_ID)).thenThrow(new ResourceNotFoundException("Ticket introuvable"));

        mockMvc.perform(get("/api/tickets/{id}", TICKET_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Ticket introuvable"));
    }

    @Test
    void assignTicketReturnsOk() throws Exception {
        when(ticketService.assignTicket(eq(TICKET_ID), any())).thenReturn(response(TicketStatus.IN_PROGRESS));

        mockMvc.perform(put("/api/tickets/{id}/assign", TICKET_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"assignedToUserId":"%s","changedByUserId":"%s"}
                                """.formatted(SUPPORT_ID, SUPPORT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void changeStatusReturnsOk() throws Exception {
        when(ticketService.changeStatus(eq(TICKET_ID), any())).thenReturn(response(TicketStatus.RESOLVED));

        mockMvc.perform(put("/api/tickets/{id}/status", TICKET_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"RESOLVED","changedByUserId":"%s"}
                                """.formatted(SUPPORT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));
    }

    @Test
    void invalidTransitionReturnsConflict() throws Exception {
        when(ticketService.changeStatus(eq(TICKET_ID), any()))
                .thenThrow(new InvalidTicketTransitionException(TicketStatus.CLOSED, TicketStatus.IN_PROGRESS));

        mockMvc.perform(put("/api/tickets/{id}/status", TICKET_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"IN_PROGRESS","changedByUserId":"%s"}
                                """.formatted(SUPPORT_ID)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void escalateTicketReturnsOk() throws Exception {
        when(ticketService.escalateTicket(eq(TICKET_ID), any())).thenReturn(response(TicketStatus.ESCALATED));

        mockMvc.perform(put("/api/tickets/{id}/escalate", TICKET_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetUserId":"%s","changedByUserId":"%s","reason":"Expertise N2 requise"}
                                """.formatted(SUPPORT_ID, SUPPORT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ESCALATED"));
    }

    @Test
    void getHistoryReturnsChronologicalJson() throws Exception {
        Instant createdAt = Instant.parse("2026-08-20T15:05:27Z");
        UserSummary changedBy = new UserSummary(
                SUPPORT_ID, "Nicolas", "Support", "nicolas.n1@supportflow.local", Role.SUPPORT_N1);
        TicketHistoryResponse history = new TicketHistoryResponse(
                UUID.randomUUID(), "status", "IN_PROGRESS", "RESOLVED", changedBy, createdAt);
        when(ticketService.getTicketHistory(TICKET_ID)).thenReturn(List.of(history));

        mockMvc.perform(get("/api/tickets/{ticketId}/history", TICKET_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fieldName").value("status"))
                .andExpect(jsonPath("$[0].oldValue").value("IN_PROGRESS"))
                .andExpect(jsonPath("$[0].newValue").value("RESOLVED"))
                .andExpect(jsonPath("$[0].changedBy.id").value(SUPPORT_ID.toString()))
                .andExpect(jsonPath("$[0].changedBy.role").value("SUPPORT_N1"))
                .andExpect(jsonPath("$[0].createdAt").value("2026-08-20T15:05:27Z"));
    }

    @Test
    void getHistoryReturnsNotFoundForMissingTicket() throws Exception {
        when(ticketService.getTicketHistory(TICKET_ID))
                .thenThrow(new ResourceNotFoundException("Ticket introuvable"));

        mockMvc.perform(get("/api/tickets/{ticketId}/history", TICKET_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Ticket introuvable"));
    }

    @Test
    void postCommentReturnsCreated() throws Exception {
        CommentResponse response = commentResponse(false);
        when(commentService.addComment(eq(TICKET_ID), any())).thenReturn(response);

        mockMvc.perform(post("/api/tickets/{ticketId}/comments", TICKET_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "authorUserId": "%s",
                                  "content": "Le problème apparaît lors du paiement.",
                                  "internal": false
                                }
                                """.formatted(USER_ID)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value("Le problème apparaît lors du paiement."))
                .andExpect(jsonPath("$.internal").value(false))
                .andExpect(jsonPath("$.author.id").value(USER_ID.toString()));
    }

    @Test
    void getCommentsReturnsOk() throws Exception {
        when(commentService.getComments(TICKET_ID)).thenReturn(List.of(commentResponse(true)));

        mockMvc.perform(get("/api/tickets/{ticketId}/comments", TICKET_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].content").value("Le problème apparaît lors du paiement."))
                .andExpect(jsonPath("$[0].internal").value(true))
                .andExpect(jsonPath("$[0].createdAt").exists());
    }

    @Test
    void blankCommentContentReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/tickets/{ticketId}/comments", TICKET_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"authorUserId":"%s","content":"   ","internal":false}
                                """.formatted(USER_ID)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.content").exists());
    }

    @Test
    void commentBusinessRuleReturnsUnprocessableEntity() throws Exception {
        when(commentService.addComment(eq(TICKET_ID), any()))
                .thenThrow(new BusinessRuleException("Une note interne est réservée à l'équipe support"));

        mockMvc.perform(post("/api/tickets/{ticketId}/comments", TICKET_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"authorUserId":"%s","content":"Note interne","internal":true}
                                """.formatted(USER_ID)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422));
    }

    @Test
    void getSlaReturnsOk() throws Exception {
        Instant responseDue = Instant.parse("2026-08-21T10:30:00Z");
        Instant resolutionDue = Instant.parse("2026-08-21T14:00:00Z");
        SlaSummaryResponse response = new SlaSummaryResponse(responseDue, resolutionDue,
                Instant.parse("2026-08-21T10:10:00Z"), null, SlaStatus.COMPLETED,
                SlaStatus.ON_TIME, 0L, 12540L);
        when(slaService.getTicketSla(TICKET_ID)).thenReturn(response);

        mockMvc.perform(get("/api/tickets/{ticketId}/sla", TICKET_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseStatus").value("COMPLETED"))
                .andExpect(jsonPath("$.resolutionStatus").value("ON_TIME"))
                .andExpect(jsonPath("$.resolutionRemainingSeconds").value(12540));
    }

    @Test
    void getSlaReturnsNotFound() throws Exception {
        when(slaService.getTicketSla(TICKET_ID)).thenThrow(new ResourceNotFoundException("Ticket introuvable"));
        mockMvc.perform(get("/api/tickets/{ticketId}/sla", TICKET_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Ticket introuvable"));
    }

    private TicketResponse response(TicketStatus status) {
        return new TicketResponse(TICKET_ID, "INC-000001", "Incident", "Description", status,
                TicketPriority.HIGH, null, null, null, Instant.now(), Instant.now(), null, null,
                null, Instant.now(), Instant.now());
    }

    private CommentResponse commentResponse(boolean internal) {
        Instant now = Instant.parse("2026-08-21T10:00:00Z");
        UserSummary author = new UserSummary(USER_ID, "Alice", "Martin",
                "alice.user@supportflow.local", Role.USER);
        return new CommentResponse(UUID.randomUUID(), "Le problème apparaît lors du paiement.",
                internal, author, now, now);
    }

    private String createJson() {
        return """
                {
                  "title": "Impossible de se connecter",
                  "description": "L'application refuse mes identifiants",
                  "priority": "HIGH",
                  "categoryId": "%s",
                  "createdByUserId": "%s"
                }
                """.formatted(CATEGORY_ID, USER_ID);
    }
}
