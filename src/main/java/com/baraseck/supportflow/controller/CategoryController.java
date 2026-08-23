package com.baraseck.supportflow.controller;

import com.baraseck.supportflow.dto.CategorySummary;
import com.baraseck.supportflow.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService categoryService;

    @GetMapping
    public List<CategorySummary> getActiveCategories() {
        return categoryService.getActiveCategories();
    }
}
