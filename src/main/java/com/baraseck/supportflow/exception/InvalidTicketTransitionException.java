package com.baraseck.supportflow.exception;

import com.baraseck.supportflow.entity.TicketStatus;

public class InvalidTicketTransitionException extends BusinessRuleException {
    public InvalidTicketTransitionException(TicketStatus from, TicketStatus to) {
        super("Transition de statut non autorisée : " + from + " vers " + to);
    }
}
