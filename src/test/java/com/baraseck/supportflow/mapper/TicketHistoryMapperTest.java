package com.baraseck.supportflow.mapper;

import com.baraseck.supportflow.dto.TicketHistoryResponse;
import com.baraseck.supportflow.entity.Role;
import com.baraseck.supportflow.entity.TicketHistory;
import com.baraseck.supportflow.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TicketHistoryMapperTest {

    private final TicketHistoryMapper mapper = new TicketHistoryMapper(new UserSummaryMapper());

    @Test
    void mapsChangedByAndHistoryValues() {
        UUID historyId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-08-20T15:05:27Z");
        User changedBy = new User();
        ReflectionTestUtils.setField(changedBy, "id", userId);
        changedBy.setFirstName("Nicolas");
        changedBy.setLastName("Support");
        changedBy.setEmail("nicolas.n1@supportflow.local");
        changedBy.setRole(Role.SUPPORT_N1);
        TicketHistory history = new TicketHistory();
        history.setId(historyId);
        history.setFieldName("status");
        history.setOldValue("IN_PROGRESS");
        history.setNewValue("RESOLVED");
        history.setChangedBy(changedBy);
        history.setCreatedAt(createdAt);

        TicketHistoryResponse response = mapper.toResponse(history);

        assertThat(response.id()).isEqualTo(historyId);
        assertThat(response.fieldName()).isEqualTo("status");
        assertThat(response.oldValue()).isEqualTo("IN_PROGRESS");
        assertThat(response.newValue()).isEqualTo("RESOLVED");
        assertThat(response.createdAt()).isEqualTo(createdAt);
        assertThat(response.changedBy().id()).isEqualTo(userId);
        assertThat(response.changedBy().firstName()).isEqualTo("Nicolas");
        assertThat(response.changedBy().role()).isEqualTo(Role.SUPPORT_N1);
    }
}
