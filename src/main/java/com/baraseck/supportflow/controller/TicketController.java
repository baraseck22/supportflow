package com.baraseck.supportflow.controller;

import com.baraseck.supportflow.dto.AssignTicketRequest;
import com.baraseck.supportflow.dto.ChangeTicketStatusRequest;
import com.baraseck.supportflow.dto.CreateTicketRequest;
import com.baraseck.supportflow.dto.EscalateTicketRequest;
import com.baraseck.supportflow.dto.TicketResponse;
import com.baraseck.supportflow.dto.TicketHistoryResponse;
import com.baraseck.supportflow.dto.AddCommentRequest;
import com.baraseck.supportflow.dto.CommentResponse;
import com.baraseck.supportflow.dto.SlaSummaryResponse;
import com.baraseck.supportflow.entity.TicketPriority;
import com.baraseck.supportflow.entity.TicketStatus;
import com.baraseck.supportflow.service.TicketService;
import com.baraseck.supportflow.service.CommentService;
import com.baraseck.supportflow.service.SlaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.List;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
@Tag(name = "Tickets", description = "Gestion du cycle de vie des incidents SupportFlow")
public class TicketController {

    private final TicketService ticketService;
    private final CommentService commentService;
    private final SlaService slaService;

    @PostMapping
    @Operation(summary = "Créer un ticket")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Ticket créé",
                    content = @Content(schema = @Schema(implementation = TicketResponse.class))),
            @ApiResponse(responseCode = "400", description = "Requête invalide"),
            @ApiResponse(responseCode = "404", description = "Utilisateur ou catégorie introuvable")
    })
    public ResponseEntity<TicketResponse> create(@Valid @RequestBody CreateTicketRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ticketService.createTicket(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulter un ticket par son UUID")
    public TicketResponse getById(@PathVariable UUID id) {
        return ticketService.getTicketById(id);
    }

    @GetMapping("/number/{ticketNumber}")
    @Operation(summary = "Consulter un ticket par son numéro lisible", description = "Exemple : INC-000001")
    public TicketResponse getByNumber(@PathVariable String ticketNumber) {
        return ticketService.getTicketByNumber(ticketNumber);
    }

    @GetMapping("/{ticketId}/history")
    @Operation(summary = "Consulter l'historique chronologique d'un ticket")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Historique du ticket",
                    content = @Content(array = @ArraySchema(
                            schema = @Schema(implementation = TicketHistoryResponse.class)))),
            @ApiResponse(responseCode = "404", description = "Ticket introuvable")
    })
    public List<TicketHistoryResponse> getHistory(@PathVariable UUID ticketId) {
        return ticketService.getTicketHistory(ticketId);
    }

    @PostMapping("/{ticketId}/comments")
    @Operation(summary = "Ajouter un commentaire public ou une note interne")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Commentaire ajouté",
                    content = @Content(schema = @Schema(implementation = CommentResponse.class))),
            @ApiResponse(responseCode = "400", description = "Requête invalide"),
            @ApiResponse(responseCode = "404", description = "Ticket ou utilisateur introuvable"),
            @ApiResponse(responseCode = "422", description = "Règle métier violée")
    })
    public ResponseEntity<CommentResponse> addComment(
            @PathVariable UUID ticketId, @Valid @RequestBody AddCommentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(commentService.addComment(ticketId, request));
    }

    @GetMapping("/{ticketId}/comments")
    @Operation(summary = "Consulter les commentaires chronologiques d'un ticket",
            description = "En développement, retourne temporairement les commentaires publics et les notes internes.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Commentaires du ticket",
                    content = @Content(array = @ArraySchema(
                            schema = @Schema(implementation = CommentResponse.class)))),
            @ApiResponse(responseCode = "404", description = "Ticket introuvable")
    })
    public List<CommentResponse> getComments(@PathVariable UUID ticketId) {
        return commentService.getComments(ticketId);
    }

    @GetMapping("/{ticketId}/sla")
    @Operation(summary = "Consulter les objectifs et statuts SLA d'un ticket")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Synthèse SLA",
                    content = @Content(schema = @Schema(implementation = SlaSummaryResponse.class))),
            @ApiResponse(responseCode = "404", description = "Ticket introuvable")
    })
    public SlaSummaryResponse getSla(@PathVariable UUID ticketId) {
        return slaService.getTicketSla(ticketId);
    }

    @GetMapping
    @Operation(summary = "Lister et filtrer les tickets",
            description = "Recherche par numéro ou titre et filtres combinables, avec pagination et tri.")
    public Page<TicketResponse> findAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) TicketPriority priority,
            @RequestParam(required = false) UUID assignedTo,
            @RequestParam(defaultValue = "false") boolean unassigned,
            @RequestParam(required = false) UUID createdBy,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ticketService.findTickets(search, status, priority, assignedTo, unassigned, createdBy, pageable);
    }

    @PutMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('SUPPORT_N1','ADMIN')")
    @Operation(summary = "Affecter un ticket à un membre du support")
    public TicketResponse assign(@PathVariable UUID id, @Valid @RequestBody AssignTicketRequest request) {
        return ticketService.assignTicket(id, request);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SUPPORT_N1','SUPPORT_N2','ADMIN')")
    @Operation(summary = "Modifier le statut d'un ticket")
    @ApiResponse(responseCode = "409", description = "Transition de statut non autorisée")
    public TicketResponse changeStatus(
            @PathVariable UUID id, @Valid @RequestBody ChangeTicketStatusRequest request) {
        return ticketService.changeStatus(id, request);
    }

    @PutMapping("/{id}/escalate")
    @PreAuthorize("hasAnyRole('SUPPORT_N1','ADMIN')")
    @Operation(summary = "Escalader un ticket du support N1 vers le support N2")
    public TicketResponse escalate(
            @PathVariable UUID id, @Valid @RequestBody EscalateTicketRequest request) {
        return ticketService.escalateTicket(id, request);
    }
}
