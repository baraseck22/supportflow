package com.baraseck.supportflow.dto;

import com.baraseck.supportflow.entity.Role;

import java.util.UUID;

public record UserSummary(UUID id, String firstName, String lastName, String email, Role role) {
}
