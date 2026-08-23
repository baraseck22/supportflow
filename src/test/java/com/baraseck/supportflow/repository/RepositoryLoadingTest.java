package com.baraseck.supportflow.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@DataJpaTest
class RepositoryLoadingTest {

    @Autowired private UserRepository userRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private TicketRepository ticketRepository;
    @Autowired private CommentRepository commentRepository;
    @Autowired private TicketHistoryRepository ticketHistoryRepository;

    @Test
    void repositoriesAreLoaded() {
        assertThat(userRepository).isNotNull();
        assertThat(categoryRepository).isNotNull();
        assertThat(ticketRepository).isNotNull();
        assertThat(commentRepository).isNotNull();
        assertThat(ticketHistoryRepository).isNotNull();
    }
}
