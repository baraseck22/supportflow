package com.baraseck.supportflow.service;

import com.baraseck.supportflow.entity.Role;
import com.baraseck.supportflow.entity.User;
import com.baraseck.supportflow.mapper.UserSummaryMapper;
import com.baraseck.supportflow.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SupportAgentServiceTest {
    @Test
    void returnsOnlyTheActiveSupportUsersSelectedByTheRepositoryWithoutPasswordHash() {
        UserRepository repository = mock(UserRepository.class);
        User agent = new User();
        agent.setFirstName("Nicolas"); agent.setLastName("Support");
        agent.setEmail("nicolas@supportflow.local"); agent.setRole(Role.SUPPORT_N1); agent.setActive(true);
        agent.setPasswordHash("must-not-be-exposed");
        when(repository.findByActiveTrueAndRoleInOrderByFirstNameAscLastNameAsc(anyCollection())).thenReturn(List.of(agent));
        var result = new SupportAgentService(repository, new UserSummaryMapper()).getActiveSupportAgents();
        assertThat(result).singleElement().satisfies(summary -> {
            assertThat(summary.firstName()).isEqualTo("Nicolas");
            assertThat(summary.role()).isEqualTo(Role.SUPPORT_N1);
            assertThat(summary.toString()).doesNotContain("must-not-be-exposed");
        });
    }
}
