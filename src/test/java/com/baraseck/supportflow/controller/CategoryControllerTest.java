package com.baraseck.supportflow.controller;

import com.baraseck.supportflow.dto.CategorySummary;
import com.baraseck.supportflow.security.DatabaseUserDetailsService;
import com.baraseck.supportflow.security.JwtAuthenticationFilter;
import com.baraseck.supportflow.security.JwtService;
import com.baraseck.supportflow.security.SecurityConfig;
import com.baraseck.supportflow.service.CategoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoryController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class CategoryControllerTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean CategoryService categoryService;
    @MockitoBean JwtService jwtService;
    @MockitoBean DatabaseUserDetailsService databaseUserDetailsService;

    @Test void withoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/categories")).andExpect(status().isUnauthorized());
    }

    @Test @WithMockUser(roles = "USER") void authenticatedUserGetsCategorySummaries() throws Exception {
        when(categoryService.getActiveCategories()).thenReturn(List.of(new CategorySummary(UUID.randomUUID(), "ACCESS", "Accès")));
        mockMvc.perform(get("/api/categories")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("ACCESS"))
                .andExpect(jsonPath("$[0].description").value("Accès"))
                .andExpect(jsonPath("$[0].active").doesNotExist());
    }
}
