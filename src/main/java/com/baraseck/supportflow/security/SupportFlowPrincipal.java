package com.baraseck.supportflow.security;

import com.baraseck.supportflow.entity.Role;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public record SupportFlowPrincipal(UUID id, String email, String password, Role role, boolean active)
        implements UserDetails {
    @Override public Collection<SimpleGrantedAuthority> getAuthorities() { return List.of(new SimpleGrantedAuthority("ROLE_" + role.name())); }
    @Override public String getUsername() { return email; }
    @Override public String getPassword() { return password; }
    @Override public boolean isEnabled() { return active; }
}
