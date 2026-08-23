package com.baraseck.supportflow.service;

import com.baraseck.supportflow.dto.DashboardSummaryResponse;
import com.baraseck.supportflow.dto.DashboardTicketSummary;
import com.baraseck.supportflow.entity.*;
import com.baraseck.supportflow.repository.CategoryRepository;
import com.baraseck.supportflow.repository.TicketRepository;
import com.baraseck.supportflow.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DashboardServiceIntegrationTest {
    @Autowired private DashboardService service;
    @Autowired private TicketRepository tickets;
    @Autowired private UserRepository users;
    @Autowired private CategoryRepository categories;
    @MockitoBean private Clock clock;
    private User support; private Category category;

    @BeforeEach void setUp(){when(clock.instant()).thenReturn(Instant.parse("2026-08-21T10:00:00Z"));support=users.save(user());category=categories.save(category());
        save("INC-000201",TicketStatus.NEW,TicketPriority.CRITICAL,null,0,"2026-08-21T09:50:00Z","2026-08-21T09:40:00Z",null,null);
        save("INC-000202",TicketStatus.IN_PROGRESS,TicketPriority.HIGH,support,1,"2026-08-21T10:10:00Z","2026-08-21T10:10:00Z",null,null);
        save("INC-000203",TicketStatus.WAITING,TicketPriority.MEDIUM,support,2,"2026-08-21T12:00:00Z","2026-08-21T12:00:00Z","2026-08-21T09:30:00Z",null);
        save("INC-000204",TicketStatus.ESCALATED,TicketPriority.MEDIUM,support,3,"2026-08-21T12:00:00Z","2026-08-21T12:00:00Z","2026-08-21T09:30:00Z",null);
        save("INC-000205",TicketStatus.RESOLVED,TicketPriority.CRITICAL,null,4,"2026-08-21T09:00:00Z","2026-08-21T09:00:00Z",null,"2026-08-21T09:00:00Z");
        save("INC-000206",TicketStatus.CLOSED,TicketPriority.CRITICAL,null,5,"2026-08-21T09:00:00Z","2026-08-21T09:00:00Z",null,null);
    }

    @Test void calculatesControlledKpisAndExcludesResolvedAndClosed(){DashboardSummaryResponse s=service.getSummary();assertThat(s.totalOpen()).isEqualTo(4);assertThat(s.newTickets()).isEqualTo(1);assertThat(s.inProgress()).isEqualTo(1);assertThat(s.pending()).isEqualTo(1);assertThat(s.escalated()).isEqualTo(1);assertThat(s.criticalOpen()).isEqualTo(1);assertThat(s.unassigned()).isEqualTo(1);}
    @Test void reusesBreachedAndAtRiskSlaRules(){DashboardSummaryResponse s=service.getSummary();assertThat(s.responseSlaBreached()).isEqualTo(1);assertThat(s.responseSlaAtRisk()).isEqualTo(1);assertThat(s.resolutionSlaBreached()).isEqualTo(1);assertThat(s.resolutionSlaAtRisk()).isEqualTo(1);}
    @Test void recentTicketsAreLimitedAndSorted(){assertThat(service.getSummary().recentTickets()).hasSize(5).extracting(DashboardTicketSummary::ticketNumber).containsExactly("INC-000206","INC-000205","INC-000204","INC-000203","INC-000202");}
    @Test void priorityTicketsPutBreachedCriticalUnassignedFirst(){assertThat(service.getSummary().priorityTickets()).isNotEmpty().first().extracting(DashboardTicketSummary::ticketNumber).isEqualTo("INC-000201");}

    private void save(String number,TicketStatus status,TicketPriority priority,User assignee,long createdMinute,String responseDue,String resolutionDue,String firstResponse,String resolved){Ticket t=new Ticket();t.setTicketNumber(number);t.setTitle("Ticket "+number);t.setDescription("Description");t.setStatus(status);t.setPriority(priority);t.setCreatedBy(support);t.setAssignedTo(assignee);t.setCategory(category);t.initializeTimestamps(Instant.parse("2026-08-21T09:0"+createdMinute+":00Z"));t.setResponseDueAt(Instant.parse(responseDue));t.setResolutionDueAt(Instant.parse(resolutionDue));if(firstResponse!=null)t.setFirstResponseAt(Instant.parse(firstResponse));if(resolved!=null)t.setResolvedAt(Instant.parse(resolved));tickets.save(t);}
    private User user(){User u=new User();u.setFirstName("Nicolas");u.setLastName("Support");u.setEmail("dashboard@test.local");u.setPasswordHash("test");u.setRole(Role.SUPPORT_N1);u.setActive(true);return u;}
    private Category category(){Category c=new Category();c.setName("DASHBOARD_TEST");c.setActive(true);return c;}
}
