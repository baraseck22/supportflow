package com.baraseck.supportflow.repository;

import com.baraseck.supportflow.entity.Ticket;
import com.baraseck.supportflow.entity.TicketPriority;
import com.baraseck.supportflow.entity.TicketStatus;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;
import java.util.UUID;

public final class TicketSpecifications {
    private TicketSpecifications() {}

    public static Specification<Ticket> textContains(String search) {
        if (search == null || search.isBlank()) return null;
        String pattern = "%" + escapeLike(search.trim().toLowerCase(Locale.ROOT)) + "%";
        return (root, query, builder) -> builder.or(
                builder.like(builder.lower(root.get("ticketNumber")), pattern, '\\'),
                builder.like(builder.lower(root.get("title")), pattern, '\\'));
    }

    public static Specification<Ticket> hasStatus(TicketStatus status) {
        return status == null ? null : (root, query, builder) -> builder.equal(root.get("status"), status);
    }

    public static Specification<Ticket> hasPriority(TicketPriority priority) {
        return priority == null ? null : (root, query, builder) -> builder.equal(root.get("priority"), priority);
    }

    public static Specification<Ticket> assignedTo(UUID id) {
        return id == null ? null : (root, query, builder) -> builder.equal(root.get("assignedTo").get("id"), id);
    }

    public static Specification<Ticket> isUnassigned(boolean value) {
        return !value ? null : (root, query, builder) -> builder.isNull(root.get("assignedTo"));
    }

    public static Specification<Ticket> createdBy(UUID id) {
        return id == null ? null : (root, query, builder) -> builder.equal(root.get("createdBy").get("id"), id);
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
