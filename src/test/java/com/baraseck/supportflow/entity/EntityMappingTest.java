package com.baraseck.supportflow.entity;

import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.EntityType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@DataJpaTest
class EntityMappingTest {

    @Autowired
    private EntityManager entityManager;

    @Test
    void allDomainEntitiesAreMapped() {
        Set<Class<?>> mappedTypes = entityManager.getMetamodel().getEntities().stream()
                .map(EntityType::getJavaType)
                .collect(Collectors.toSet());

        assertThat(mappedTypes).contains(User.class, Category.class, Ticket.class,
                Comment.class, TicketHistory.class);
    }

    @Test
    void enumsExposeExpectedValues() {
        assertThat(Role.values()).containsExactly(Role.USER, Role.SUPPORT_N1, Role.SUPPORT_N2, Role.ADMIN);
        assertThat(TicketStatus.values()).containsExactly(TicketStatus.NEW, TicketStatus.IN_PROGRESS,
                TicketStatus.WAITING, TicketStatus.ESCALATED, TicketStatus.RESOLVED, TicketStatus.CLOSED);
        assertThat(TicketPriority.values()).containsExactly(TicketPriority.LOW, TicketPriority.MEDIUM,
                TicketPriority.HIGH, TicketPriority.CRITICAL);
    }
}
