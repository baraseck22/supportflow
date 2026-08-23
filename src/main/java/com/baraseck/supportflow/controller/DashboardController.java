package com.baraseck.supportflow.controller;

import com.baraseck.supportflow.dto.DashboardSummaryResponse;
import com.baraseck.supportflow.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Vue opérationnelle réservée à l'équipe support")
public class DashboardController {
    private final DashboardService dashboardService;

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('SUPPORT_N1','SUPPORT_N2','ADMIN')")
    @Operation(summary = "Consulter la synthèse opérationnelle du support")
    public DashboardSummaryResponse getSummary() {
        return dashboardService.getSummary();
    }
}
