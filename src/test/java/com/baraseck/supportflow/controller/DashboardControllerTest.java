package com.baraseck.supportflow.controller;

import com.baraseck.supportflow.dto.DashboardSummaryResponse;
import com.baraseck.supportflow.security.DatabaseUserDetailsService;
import com.baraseck.supportflow.security.JwtAuthenticationFilter;
import com.baraseck.supportflow.security.JwtService;
import com.baraseck.supportflow.security.SecurityConfig;
import com.baraseck.supportflow.service.DashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DashboardController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class DashboardControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockitoBean private DashboardService dashboardService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private DatabaseUserDetailsService databaseUserDetailsService;

    @Test void anonymousIsUnauthorized() throws Exception { mockMvc.perform(get("/api/dashboard/summary")).andExpect(status().isUnauthorized()); }
    @Test @WithMockUser(roles="USER") void userIsForbidden() throws Exception { mockMvc.perform(get("/api/dashboard/summary")).andExpect(status().isForbidden()); }
    @Test @WithMockUser(roles="SUPPORT_N1") void n1IsAuthorized() throws Exception { assertAuthorized(); }
    @Test @WithMockUser(roles="SUPPORT_N2") void n2IsAuthorized() throws Exception { assertAuthorized(); }
    @Test @WithMockUser(roles="ADMIN") void adminIsAuthorized() throws Exception { assertAuthorized(); }

    private void assertAuthorized() throws Exception {
        when(dashboardService.getSummary()).thenReturn(new DashboardSummaryResponse(4,1,1,1,1,1,1,1,1,1,1,List.of(),List.of()));
        mockMvc.perform(get("/api/dashboard/summary")).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalOpen").value(4)).andExpect(jsonPath("$.pending").value(1));
    }
}
