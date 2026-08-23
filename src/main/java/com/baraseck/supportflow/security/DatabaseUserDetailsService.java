package com.baraseck.supportflow.security;

import com.baraseck.supportflow.entity.User;
import com.baraseck.supportflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DatabaseUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;
    @Override public UserDetails loadUserByUsername(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("Identifiants invalides"));
        return new SupportFlowPrincipal(user.getId(), user.getEmail(), user.getPasswordHash(), user.getRole(), user.isActive());
    }
}
