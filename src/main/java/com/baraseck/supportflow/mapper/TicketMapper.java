package com.baraseck.supportflow.mapper;

import com.baraseck.supportflow.dto.CategorySummary;
import com.baraseck.supportflow.dto.TicketResponse;
import com.baraseck.supportflow.entity.Category;
import com.baraseck.supportflow.entity.Ticket;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TicketMapper {

    private final UserSummaryMapper userSummaryMapper;

    public TicketResponse toResponse(Ticket ticket) {
        return new TicketResponse(
                ticket.getId(), ticket.getTicketNumber(), ticket.getTitle(), ticket.getDescription(),
                ticket.getStatus(), ticket.getPriority(), toCategorySummary(ticket.getCategory()),
                userSummaryMapper.toSummary(ticket.getCreatedBy()), userSummaryMapper.toSummary(ticket.getAssignedTo()),
                ticket.getCreatedAt(), ticket.getUpdatedAt(), ticket.getResolvedAt(), ticket.getClosedAt(),
                ticket.getFirstResponseAt(), ticket.getResponseDueAt(), ticket.getResolutionDueAt());
    }

    private CategorySummary toCategorySummary(Category category) {
        return new CategorySummary(category.getId(), category.getName(), category.getDescription());
    }
}
