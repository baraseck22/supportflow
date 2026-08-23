package com.baraseck.supportflow.service;

import com.baraseck.supportflow.entity.Category;
import com.baraseck.supportflow.repository.CategoryRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CategoryServiceTest {
    @Test
    void returnsTheActiveCategoriesSelectedAndSortedByTheRepository() {
        CategoryRepository repository = mock(CategoryRepository.class);
        Category category = new Category(); category.setName("ACCESS"); category.setDescription("Accès"); category.setActive(true);
        when(repository.findByActiveTrueOrderByNameAsc()).thenReturn(List.of(category));
        var result = new CategoryService(repository).getActiveCategories();
        verify(repository).findByActiveTrueOrderByNameAsc();
        assertThat(result).singleElement().satisfies(summary -> {
            assertThat(summary.name()).isEqualTo("ACCESS");
            assertThat(summary.description()).isEqualTo("Accès");
        });
    }
}
