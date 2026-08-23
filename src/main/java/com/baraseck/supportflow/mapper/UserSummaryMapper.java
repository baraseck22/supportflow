package com.baraseck.supportflow.mapper;

import com.baraseck.supportflow.dto.UserSummary;
import com.baraseck.supportflow.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserSummaryMapper {

    public UserSummary toSummary(User user) {
        return user == null ? null : new UserSummary(
                user.getId(), user.getFirstName(), user.getLastName(), user.getEmail(), user.getRole());
    }
}
