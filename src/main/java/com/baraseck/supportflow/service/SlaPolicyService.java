package com.baraseck.supportflow.service;

import com.baraseck.supportflow.entity.TicketPriority;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class SlaPolicyService {
    public Duration getResponseTarget(TicketPriority priority) {
        return switch (priority) {
            case CRITICAL -> Duration.ofMinutes(15);
            case HIGH -> Duration.ofMinutes(30);
            case MEDIUM -> Duration.ofHours(2);
            case LOW -> Duration.ofHours(4);
        };
    }

    public Duration getResolutionTarget(TicketPriority priority) {
        return switch (priority) {
            case CRITICAL -> Duration.ofHours(2);
            case HIGH -> Duration.ofHours(4);
            case MEDIUM -> Duration.ofHours(8);
            case LOW -> Duration.ofHours(24);
        };
    }
}
