package com.baraseck.supportflow.controller;

import com.baraseck.supportflow.dto.UserSummary;
import com.baraseck.supportflow.entity.Role;
import com.baraseck.supportflow.security.DatabaseUserDetailsService;
import com.baraseck.supportflow.security.JwtService;
import com.baraseck.supportflow.security.JwtAuthenticationFilter;
import com.baraseck.supportflow.security.SecurityConfig;
import com.baraseck.supportflow.service.SupportAgentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class UserControllerTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean SupportAgentService service;
    @MockitoBean JwtService jwtService;
    @MockitoBean DatabaseUserDetailsService databaseUserDetailsService;

    @Test void withoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/users/support-agents")).andExpect(status().isUnauthorized());
    }

    @Test @WithMockUser(roles = "USER") void userReturns403() throws Exception {
        mockMvc.perform(get("/api/users/support-agents")).andExpect(status().isForbidden());
    }

    @Test @WithMockUser(roles = "SUPPORT_N1") void n1ReturnsAgentsWithoutPassword() throws Exception { assertAllowed(); }
    @Test @WithMockUser(roles = "SUPPORT_N2") void n2ReturnsAgentsWithoutPassword() throws Exception { assertAllowed(); }
    @Test @WithMockUser(roles = "ADMIN") void adminReturnsAgentsWithoutPassword() throws Exception { assertAllowed(); }

    private void assertAllowed() throws Exception {
        when(service.getActiveSupportAgents()).thenReturn(List.of(new UserSummary(UUID.randomUUID(), "Nicolas", "Support", "nicolas@supportflow.local", Role.SUPPORT_N1)));
        mockMvc.perform(get("/api/users/support-agents")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].role").value("SUPPORT_N1"))
                .andExpect(jsonPath("$[0].passwordHash").doesNotExist());
    }
}
