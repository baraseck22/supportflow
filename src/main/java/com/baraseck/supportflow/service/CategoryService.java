package com.baraseck.supportflow.service;

import com.baraseck.supportflow.dto.CategorySummary;
import com.baraseck.supportflow.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<CategorySummary> getActiveCategories() {
        return categoryRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(category -> new CategorySummary(category.getId(), category.getName(), category.getDescription()))
                .toList();
    }
}
