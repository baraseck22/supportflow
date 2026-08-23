package com.baraseck.supportflow.repository;

import com.baraseck.supportflow.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
    Optional<Category> findByNameIgnoreCase(String name);
    List<Category> findByActiveTrueOrderByNameAsc();
}
