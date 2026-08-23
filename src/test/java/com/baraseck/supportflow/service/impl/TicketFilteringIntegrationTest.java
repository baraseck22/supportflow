package com.baraseck.supportflow.service.impl;

import com.baraseck.supportflow.dto.TicketResponse;
import com.baraseck.supportflow.entity.*;
import com.baraseck.supportflow.repository.CategoryRepository;
import com.baraseck.supportflow.repository.TicketRepository;
import com.baraseck.supportflow.repository.UserRepository;
import com.baraseck.supportflow.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TicketFilteringIntegrationTest {
    @Autowired private TicketServiceImpl service;
    @Autowired private TicketRepository tickets;
    @Autowired private UserRepository users;
    @Autowired private CategoryRepository categories;
    @MockitoBean private CurrentUserService currentUserService;

    private User alice;
    private User bob;
    private User nicolas;
    private User sophie;

    @BeforeEach
    void setUp() {
        Category category = new Category(); category.setName("APPLICATION_ERROR"); category.setActive(true);
        category = categories.save(category);
        alice = users.save(user("alice@test.local", Role.USER));
        bob = users.save(user("bob@test.local", Role.USER));
        nicolas = users.save(user("nicolas@test.local", Role.SUPPORT_N1));
        sophie = users.save(user("sophie@test.local", Role.SUPPORT_N2));
        tickets.save(ticket("INC-000101", "Tableau de paiement", TicketStatus.IN_PROGRESS, TicketPriority.HIGH, alice, nicolas, category));
        tickets.save(ticket("INC-000102", "Connexion impossible", TicketStatus.NEW, TicketPriority.LOW, bob, null, category));
        tickets.save(ticket("INC-000103", "PAIEMENT mobile", TicketStatus.IN_PROGRESS, TicketPriority.CRITICAL, bob, sophie, category));
        when(currentUserService.requireUser()).thenReturn(nicolas);
    }

    @Test void searchesByTicketNumber() { assertThat(find("000102", null, null, null, false, null).getContent()).extracting(TicketResponse::ticketNumber).containsExactly("INC-000102"); }
    @Test void searchesTitleIgnoringCase() { assertThat(find("tableau", null, null, null, false, null).getContent()).extracting(TicketResponse::ticketNumber).containsExactly("INC-000101"); }
    @Test void combinesSearchAndStatus() { assertThat(find("paiement", TicketStatus.IN_PROGRESS, null, null, false, null).getTotalElements()).isEqualTo(2); }
    @Test void combinesStatusAndPriority() { assertThat(find(null, TicketStatus.IN_PROGRESS, TicketPriority.HIGH, null, false, null).getContent()).extracting(TicketResponse::ticketNumber).containsExactly("INC-000101"); }
    @Test void combinesAssignedAgentAndStatus() { assertThat(find(null, TicketStatus.IN_PROGRESS, null, nicolas.getId(), false, null).getContent()).extracting(TicketResponse::ticketNumber).containsExactly("INC-000101"); }
    @Test void supportsUnassignedTickets() { assertThat(find(null, null, null, null, true, null).getContent()).extracting(TicketResponse::ticketNumber).containsExactly("INC-000102"); }
    @Test void preservesPaginationAndSorting() { Page<TicketResponse> page=service.findTickets(null,null,null,null,false,null,PageRequest.of(0,1,Sort.by(Sort.Direction.DESC,"ticketNumber")));assertThat(page.getTotalElements()).isEqualTo(3);assertThat(page.getTotalPages()).isEqualTo(3);assertThat(page.getContent()).extracting(TicketResponse::ticketNumber).containsExactly("INC-000103"); }
    @Test void userCannotBypassOwnershipWithSearchOrCreatedBy() { when(currentUserService.requireUser()).thenReturn(alice);Page<TicketResponse> page=find("paiement",null,null,null,false,bob.getId());assertThat(page.getContent()).extracting(TicketResponse::ticketNumber).containsExactly("INC-000101"); }
    @Test void supportKeepsAuthorizedGlobalView() { assertThat(find("paiement",null,null,null,false,null).getTotalElements()).isEqualTo(2); }

    private Page<TicketResponse> find(String search,TicketStatus status,TicketPriority priority,UUID assignedTo,boolean unassigned,UUID createdBy){return service.findTickets(search,status,priority,assignedTo,unassigned,createdBy,PageRequest.of(0,20,Sort.by("ticketNumber")));}
    private User user(String email,Role role){User user=new User();user.setFirstName("Test");user.setLastName("User");user.setEmail(email);user.setPasswordHash("test");user.setRole(role);user.setActive(true);return user;}
    private Ticket ticket(String number,String title,TicketStatus status,TicketPriority priority,User creator,User assignee,Category category){Ticket ticket=new Ticket();ticket.setTicketNumber(number);ticket.setTitle(title);ticket.setDescription("Description");ticket.setStatus(status);ticket.setPriority(priority);ticket.setCreatedBy(creator);ticket.setAssignedTo(assignee);ticket.setCategory(category);ticket.setResponseDueAt(Instant.parse("2026-08-21T10:15:00Z"));ticket.setResolutionDueAt(Instant.parse("2026-08-21T12:00:00Z"));return ticket;}
}
