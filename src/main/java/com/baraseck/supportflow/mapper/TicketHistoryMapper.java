package com.baraseck.supportflow.mapper;

import com.baraseck.supportflow.dto.TicketHistoryResponse;
import com.baraseck.supportflow.entity.TicketHistory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TicketHistoryMapper {

    private final UserSummaryMapper userSummaryMapper;

    public TicketHistoryResponse toResponse(TicketHistory history) {
        return new TicketHistoryResponse(
                history.getId(), history.getFieldName(), history.getOldValue(), history.getNewValue(),
                userSummaryMapper.toSummary(history.getChangedBy()), history.getCreatedAt());
    }
}
