package com.baraseck.supportflow.controller;

import com.baraseck.supportflow.dto.UserSummary;
import com.baraseck.supportflow.service.SupportAgentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final SupportAgentService supportAgentService;

    @GetMapping("/support-agents")
    @PreAuthorize("hasAnyRole('SUPPORT_N1','SUPPORT_N2','ADMIN')")
    public List<UserSummary> getSupportAgents() {
        return supportAgentService.getActiveSupportAgents();
    }
}
