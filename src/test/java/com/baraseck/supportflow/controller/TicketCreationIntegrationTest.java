package com.baraseck.supportflow.controller;

import com.baraseck.supportflow.entity.Category;
import com.baraseck.supportflow.entity.Role;
import com.baraseck.supportflow.entity.User;
import com.baraseck.supportflow.repository.CategoryRepository;
import com.baraseck.supportflow.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import com.baraseck.supportflow.security.JwtService;
import com.baraseck.supportflow.security.SupportFlowPrincipal;
import com.baraseck.supportflow.security.CurrentUserService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import static org.mockito.Mockito.when;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class TicketCreationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private CurrentUserService currentUserService;

    private User creator;
    private Category category;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("CREATE SEQUENCE IF NOT EXISTS ticket_number_seq START WITH 1");
        creator = new User();
        creator.setFirstName("Alice");
        creator.setLastName("Martin");
        creator.setEmail("alice.integration@supportflow.local");
        creator.setPasswordHash("integration-test-only");
        creator.setRole(Role.USER);
        creator.setActive(true);
        creator = userRepository.save(creator);
        when(currentUserService.requireUser()).thenReturn(creator);

        category = new Category();
        category.setName("APPLICATION_ERROR_INTEGRATION");
        category.setDescription("Integration test category");
        category.setActive(true);
        category = categoryRepository.save(category);
    }

    @Test
    void createCriticalTicketReturns201WithDeadlinesCalculatedFromCreatedAt() throws Exception {
        String payload = """
                {
                  "title": "API de paiement indisponible",
                  "description": "Les utilisateurs reçoivent une erreur lors de toutes les tentatives de paiement.",
                  "priority": "CRITICAL",
                  "categoryId": "%s",
                  "createdByUserId": "%s"
                }
                """.formatted(category.getId(), creator.getId());

        SupportFlowPrincipal principal = new SupportFlowPrincipal(
                creator.getId(), creator.getEmail(), creator.getPasswordHash(), creator.getRole(), true);
        var authentication = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        String response = mockMvc.perform(post("/api/tickets")
                        .with(authentication(authentication))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.responseDueAt").isNotEmpty())
                .andExpect(jsonPath("$.resolutionDueAt").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        var json = new com.fasterxml.jackson.databind.ObjectMapper().readTree(response);
        Instant createdAt = Instant.parse(json.get("createdAt").asText());
        Instant responseDueAt = Instant.parse(json.get("responseDueAt").asText());
        Instant resolutionDueAt = Instant.parse(json.get("resolutionDueAt").asText());

        assertThat(responseDueAt).isEqualTo(createdAt.plusSeconds(15 * 60));
        assertThat(resolutionDueAt).isEqualTo(createdAt.plusSeconds(2 * 60 * 60));
    }
}
