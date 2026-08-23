package com.baraseck.supportflow.service;

import com.baraseck.supportflow.dto.UserSummary;
import com.baraseck.supportflow.entity.Role;
import com.baraseck.supportflow.mapper.UserSummaryMapper;
import com.baraseck.supportflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SupportAgentService {
    private final UserRepository userRepository;
    private final UserSummaryMapper mapper;

    @Transactional(readOnly = true)
    public List<UserSummary> getActiveSupportAgents() {
        return userRepository.findByActiveTrueAndRoleInOrderByFirstNameAscLastNameAsc(
                        EnumSet.of(Role.SUPPORT_N1, Role.SUPPORT_N2, Role.ADMIN))
                .stream().map(mapper::toSummary).toList();
    }
}
