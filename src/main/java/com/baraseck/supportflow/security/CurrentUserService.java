package com.baraseck.supportflow.security;

import com.baraseck.supportflow.entity.User;
import com.baraseck.supportflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrentUserService {
    private final UserRepository userRepository;
    public User requireUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof SupportFlowPrincipal principal))
            throw new AccessDeniedException("Authentification requise");
        return userRepository.findById(principal.id()).filter(User::isActive)
                .orElseThrow(() -> new AccessDeniedException("Utilisateur inactif ou introuvable"));
    }
}
