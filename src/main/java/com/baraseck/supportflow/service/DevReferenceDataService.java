package com.baraseck.supportflow.service;

import com.baraseck.supportflow.dto.CategorySummary;
import com.baraseck.supportflow.dto.UserSummary;
import com.baraseck.supportflow.repository.CategoryRepository;
import com.baraseck.supportflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Profile("dev")
@RequiredArgsConstructor
public class DevReferenceDataService {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<UserSummary> getUsers() {
        return userRepository.findAll().stream()
                .map(user -> new UserSummary(user.getId(), user.getFirstName(), user.getLastName(),
                        user.getEmail(), user.getRole()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CategorySummary> getCategories() {
        return categoryRepository.findAll().stream()
                .map(category -> new CategorySummary(category.getId(), category.getName(), category.getDescription()))
                .toList();
    }
}
