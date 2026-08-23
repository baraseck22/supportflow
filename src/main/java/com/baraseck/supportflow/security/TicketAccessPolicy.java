package com.baraseck.supportflow.security;

import com.baraseck.supportflow.entity.Role;
import com.baraseck.supportflow.entity.Ticket;
import com.baraseck.supportflow.entity.User;
import org.springframework.security.access.AccessDeniedException;

public final class TicketAccessPolicy {
    private TicketAccessPolicy() {}
    public static void requireRead(Ticket ticket, User user) {
        if (user.getRole() == Role.USER && !ticket.getCreatedBy().getId().equals(user.getId()))
            throw new AccessDeniedException("Accès interdit à ce ticket");
    }
    public static boolean isSupport(User user) { return user.getRole() != Role.USER; }
}
