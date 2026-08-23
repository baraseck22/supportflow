package com.baraseck.supportflow.controller;

import com.baraseck.supportflow.dto.CategorySummary;
import com.baraseck.supportflow.dto.UserSummary;
import com.baraseck.supportflow.service.DevReferenceDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Profile("dev")
@RequestMapping("/api/dev")
@RequiredArgsConstructor
@Tag(name = "Données de développement", description = "Endpoints disponibles uniquement avec le profil dev")
public class DevReferenceDataController {

    private final DevReferenceDataService referenceDataService;

    @GetMapping("/users")
    @Operation(summary = "Lister les utilisateurs de développement sans leur mot de passe")
    public List<UserSummary> getUsers() {
        return referenceDataService.getUsers();
    }

    @GetMapping("/categories")
    @Operation(summary = "Lister les catégories de développement")
    public List<CategorySummary> getCategories() {
        return referenceDataService.getCategories();
    }
}
